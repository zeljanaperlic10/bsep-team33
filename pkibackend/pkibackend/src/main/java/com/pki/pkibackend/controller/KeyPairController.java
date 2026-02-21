package com.pki.pkibackend.controller;

import com.pki.pkibackend.service.KeyPairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/keypair")
public class KeyPairController {

    private static final Logger log = LoggerFactory.getLogger(KeyPairController.class);

    private final KeyPairService keyPairService;

    public KeyPairController(KeyPairService keyPairService) {
        this.keyPairService = keyPairService;
    }

    // Pomoćni metod za dobijanje emaila ulogovanog admina
    private String getCurrentAdminEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    // POST /api/keypair/generate
    // Generiše novi RSA par ključeva
    // VAŽNO: Privatni ključ se vraća SAMO JEDNOM ovde
    // Admin mora da ga sačuva lokalno kao .pem fajl
    // Ako se izgubi, mora generisati novi par (sve stare lozinke postaju nedekriptabilne)
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> generateKeyPair() {
        String email = getCurrentAdminEmail();
        try {
            log.warn("Admin {} generiše novi par ključeva — " +
                "privatni ključ će biti vraćen JEDNOM", email);
            Map<String, String> keyPair = keyPairService.generateKeyPair(email);
            return ResponseEntity.ok(keyPair);
        } catch (Exception e) {
            log.error("Greška pri generisanju para ključeva: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/keypair/public-key
    // Vraća javni ključ admina
    // Koristi se na frontendu kada treba enkriptovati lozinku
    @GetMapping("/public-key")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        String email = getCurrentAdminEmail();
        try {
            String publicKey = keyPairService.getPublicKey(email);
            return ResponseEntity.ok(Map.of("publicKey", publicKey));
        } catch (Exception e) {
            log.error("Greška pri preuzimanju javnog ključa: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/keypair/status
    // Proverava da li admin ima generisan par ključeva
    // Frontend koristi ovo da zna da li treba prikazati dugme za generisanje
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> getKeyPairStatus() {
        String email = getCurrentAdminEmail();
        boolean hasKeyPair = keyPairService.hasKeyPair(email);
        return ResponseEntity.ok(Map.of("hasKeyPair", hasKeyPair));
    }
}