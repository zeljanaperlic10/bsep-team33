package com.pki.pkibackend.controller;

import com.pki.pkibackend.dto.CertificateRequest;
import com.pki.pkibackend.dto.CertificateResponse;
import com.pki.pkibackend.service.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private static final Logger log = LoggerFactory.getLogger(CertificateController.class);

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    private String getCurrentAdminEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    // GET /api/certificates
    @GetMapping
    public ResponseEntity<List<CertificateResponse>> getAllCertificates() {
        try {
            List<CertificateResponse> certificates = certificateService.getAllCertificates();
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            log.error("Greška pri dohvatanju sertifikata: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /api/certificates/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponse> getCertificateById(@PathVariable Long id) {
        try {
            CertificateResponse cert = certificateService.getCertificateById(id);
            return ResponseEntity.ok(cert);
        } catch (Exception e) {
            log.error("Greška pri dohvatanju sertifikata {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/certificates/issuers
    @GetMapping("/issuers")
    public ResponseEntity<List<CertificateResponse>> getAvailableIssuers() {
        try {
            List<CertificateResponse> issuers = certificateService.getAvailableIssuers();
            return ResponseEntity.ok(issuers);
        } catch (Exception e) {
            log.error("Greška pri dohvatanju issuera: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // POST /api/certificates/generate
    // Uklonili smo @Valid da ne blokira request zbog validacije
    @PostMapping("/generate")
    public ResponseEntity<?> generateCertificate(@RequestBody CertificateRequest request) {
        try {
            // Logujemo šta smo primili da vidimo problem
            log.info("=== GENERATE REQUEST ===");
            log.info("Type: {}", request.getType());
            log.info("CommonName: {}", request.getCommonName());
            log.info("Organization: {}", request.getOrganization());
            log.info("ValidFrom: {}", request.getValidFrom());
            log.info("ValidTo: {}", request.getValidTo());
            log.info("IssuerAlias: {}", request.getIssuerAlias());
            log.info("isCA: {}", request.isCA());
            log.info("=======================");

            // Ručna validacija umesto @Valid
            if (request.getType() == null || request.getType().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tip sertifikata je obavezan"));
            }
            if (request.getCommonName() == null || request.getCommonName().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Common Name je obavezan"));
            }
            if (request.getOrganization() == null || request.getOrganization().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Organizacija je obavezna"));
            }
            if (request.getValidFrom() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "ValidFrom je obavezan — format: yyyy-MM-ddTHH:mm:ss"));
            }
            if (request.getValidTo() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "ValidTo je obavezan — format: yyyy-MM-ddTHH:mm:ss"));
            }
            if (!request.getType().equalsIgnoreCase("ROOT")) {
                if (request.getIssuerAlias() == null || request.getIssuerAlias().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "IssuerAlias je obavezan za " + request.getType()));
                }
            }

            log.info("Admin {} kreira {} sertifikat za: {}",
                getCurrentAdminEmail(), request.getType(), request.getCommonName());

            CertificateResponse response = certificateService.generateCertificate(request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validaciona greška: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Greška pri generisanju sertifikata: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                Map.of("message", e.getMessage() != null ? e.getMessage() : "Interna greška — pogledaj konzolu")
            );
        }
    }

    // PUT /api/certificates/{id}/revoke
    @PutMapping("/{id}/revoke")
    public ResponseEntity<?> revokeCertificate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            if (reason == null || reason.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Razlog povlačenja je obavezan"));
            }

            log.info("Admin {} povlači sertifikat ID: {} — razlog: {}",
                getCurrentAdminEmail(), id, reason);

            CertificateResponse response = certificateService.revokeCertificate(id, reason);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.error("Greška pri povlačenju: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Greška pri povlačenju sertifikata {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }
}