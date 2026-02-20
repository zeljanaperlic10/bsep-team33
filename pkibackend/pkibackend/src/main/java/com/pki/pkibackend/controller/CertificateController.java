package com.pki.pkibackend.controller;

import com.pki.pkibackend.dto.CertificateRequest;
import com.pki.pkibackend.dto.CertificateResponse;
import com.pki.pkibackend.service.CertificateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // GET /api/certificates
    // Vraća sve sertifikate u sistemu
    // Samo admin može da vidi sve sertifikate
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CertificateResponse>> getAllCertificates() {
        log.info("Admin zatražio listu svih sertifikata");
        List<CertificateResponse> certificates = certificateService.getAllCertificates();
        return ResponseEntity.ok(certificates);
    }

    // GET /api/certificates/{id}
    // Vraća jedan sertifikat po ID-u
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificateResponse> getCertificateById(@PathVariable Long id) {
        log.info("Admin zatražio sertifikat sa ID: {}", id);
        CertificateResponse certificate = certificateService.getCertificateById(id);
        return ResponseEntity.ok(certificate);
    }

    // GET /api/certificates/issuers
    // Vraća listu svih CA sertifikata koji nisu povučeni
    // Koristi se za dropdown na frontendu kada biramo issuera
    @GetMapping("/issuers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CertificateResponse>> getAvailableIssuers() {
        log.info("Admin zatražio listu dostupnih issuera");
        List<CertificateResponse> issuers = certificateService.getAvailableIssuers();
        return ResponseEntity.ok(issuers);
    }

    // POST /api/certificates/generate
    // Generiše novi sertifikat (ROOT, INTERMEDIATE ili END_ENTITY)
    // @Valid aktivira validaciju iz CertificateRequest anotacija
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificateResponse> generateCertificate(
            @Valid @RequestBody CertificateRequest request) {
        try {
            log.info("Admin kreira {} sertifikat za: {}",
                request.getType(), request.getCommonName());
            CertificateResponse response = certificateService.generateCertificate(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Greška pri generisanju sertifikata: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/certificates/{id}/revoke
    // Povlači sertifikat — prima razlog povlačenja u body-ju
    // X.509 standard zahteva navođenje razloga
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificateResponse> revokeCertificate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String reason = body.get("reason");
        if (reason == null || reason.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            log.warn("Admin povlači sertifikat ID: {} — razlog: {}", id, reason);
            CertificateResponse response = certificateService.revokeCertificate(id, reason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Greška pri povlačenju sertifikata: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}