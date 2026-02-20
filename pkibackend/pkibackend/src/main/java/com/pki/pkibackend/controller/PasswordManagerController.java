package com.pki.pkibackend.controller;

import com.pki.pkibackend.dto.PasswordEntryRequest;
import com.pki.pkibackend.dto.PasswordEntryResponse;
import com.pki.pkibackend.service.PasswordManagerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passwords")
public class PasswordManagerController {

    private static final Logger log = LoggerFactory.getLogger(PasswordManagerController.class);

    private final PasswordManagerService passwordManagerService;

    public PasswordManagerController(PasswordManagerService passwordManagerService) {
        this.passwordManagerService = passwordManagerService;
    }

    // Pomoćni metod koji izvlači email ulogovanog admina
    // iz Spring Security konteksta — ne moramo ga primati kao parametar
    private String getCurrentAdminEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    // GET /api/passwords
    // Vraća sve lozinke ulogovanog admina
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PasswordEntryResponse>> getAllPasswords() {
        String email = getCurrentAdminEmail();
        log.info("Admin {} zatražio listu lozinki", email);
        List<PasswordEntryResponse> passwords = passwordManagerService.getAllPasswords(email);
        return ResponseEntity.ok(passwords);
    }

    // GET /api/passwords/{id}
    // Vraća jednu lozinku — enkriptovana, dekripcija na frontendu
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PasswordEntryResponse> getPasswordById(@PathVariable Long id) {
        String email = getCurrentAdminEmail();
        try {
            PasswordEntryResponse response = passwordManagerService.getPasswordById(id, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Greška pri preuzimanju lozinke ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // POST /api/passwords
    // Čuva novu enkriptovanu lozinku
    // Lozinka dolazi već enkriptovana sa frontenda pomoću Web Crypto API
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PasswordEntryResponse> savePassword(
            @Valid @RequestBody PasswordEntryRequest request) {
        String email = getCurrentAdminEmail();
        try {
            PasswordEntryResponse response = passwordManagerService.savePassword(request, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Greška pri čuvanju lozinke: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/passwords/{id}
    // Ažurira postojeću lozinku
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PasswordEntryResponse> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordEntryRequest request) {
        String email = getCurrentAdminEmail();
        try {
            PasswordEntryResponse response = passwordManagerService.updatePassword(id, request, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Greška pri ažuriranju lozinke ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // DELETE /api/passwords/{id}
    // Briše lozinku
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePassword(@PathVariable Long id) {
        String email = getCurrentAdminEmail();
        try {
            passwordManagerService.deletePassword(id, email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Greška pri brisanju lozinke ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}