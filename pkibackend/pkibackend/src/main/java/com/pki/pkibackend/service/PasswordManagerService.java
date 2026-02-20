package com.pki.pkibackend.service;

import com.pki.pkibackend.dto.PasswordEntryRequest;
import com.pki.pkibackend.dto.PasswordEntryResponse;
import com.pki.pkibackend.model.PasswordEntry;
import com.pki.pkibackend.model.User;
import com.pki.pkibackend.repository.PasswordEntryRepository;
import com.pki.pkibackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordManagerService {

    private static final Logger log = LoggerFactory.getLogger(PasswordManagerService.class);

    private final PasswordEntryRepository passwordEntryRepository;
    private final UserRepository userRepository;

    public PasswordManagerService(PasswordEntryRepository passwordEntryRepository,
                                   UserRepository userRepository) {
        this.passwordEntryRepository = passwordEntryRepository;
        this.userRepository = userRepository;
    }

    // Čuva novu enkriptovanu lozinku u bazu
    // Lozinka dolazi već enkriptovana sa frontenda
    // Backend nikad ne vidi plain-text lozinku
    public PasswordEntryResponse savePassword(PasswordEntryRequest request, String adminEmail) {
        // Učitavamo admina iz baze
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen: " + adminEmail));

        // Kreiramo novi unos
        PasswordEntry entry = new PasswordEntry();
        entry.setSiteName(request.getSiteName());
        entry.setUsername(request.getUsername());
        // Čuvamo enkriptovanu lozinku onako kako je dobijamo sa frontenda
        entry.setEncryptedPassword(request.getEncryptedPassword());
        entry.setOwner(admin);

        PasswordEntry saved = passwordEntryRepository.save(entry);
        log.info("Nova lozinka sačuvana za sajt: {} od strane: {}",
            request.getSiteName(), adminEmail);

        return mapToResponse(saved);
    }

    // Vraća sve lozinke za ulogovanog admina
    // Svaki admin vidi samo svoje lozinke
    public List<PasswordEntryResponse> getAllPasswords(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen: " + adminEmail));

        List<PasswordEntry> entries = passwordEntryRepository.findByOwner(admin);
        List<PasswordEntryResponse> responses = new ArrayList<>();

        for (PasswordEntry entry : entries) {
            responses.add(mapToResponse(entry));
        }

        log.info("Admin {} zatražio listu lozinki — pronađeno: {}", adminEmail, responses.size());
        return responses;
    }

    // Vraća jednu lozinku po ID-u
    // Proveravamo da admin može videti samo svoje lozinke
    public PasswordEntryResponse getPasswordById(Long id, String adminEmail) {
        PasswordEntry entry = passwordEntryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lozinka nije pronađena sa ID: " + id));

        // Sigurnosna provera — vlasnik mora biti ulogovani admin
        if (!entry.getOwner().getEmail().equals(adminEmail)) {
            log.warn("Admin {} pokušao pristupiti tuđoj lozinci ID: {}", adminEmail, id);
            throw new RuntimeException("Nemate dozvolu za pristup ovoj lozinci");
        }

        return mapToResponse(entry);
    }

    // Ažurira postojeću lozinku
    // Nova lozinka dolazi enkriptovana sa frontenda
    public PasswordEntryResponse updatePassword(Long id, PasswordEntryRequest request,
                                                 String adminEmail) {
        PasswordEntry entry = passwordEntryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lozinka nije pronađena sa ID: " + id));

        // Sigurnosna provera
        if (!entry.getOwner().getEmail().equals(adminEmail)) {
            log.warn("Admin {} pokušao izmeniti tuđu lozinku ID: {}", adminEmail, id);
            throw new RuntimeException("Nemate dozvolu za izmenu ove lozinke");
        }

        entry.setSiteName(request.getSiteName());
        entry.setUsername(request.getUsername());
        entry.setEncryptedPassword(request.getEncryptedPassword());

        PasswordEntry updated = passwordEntryRepository.save(entry);
        log.info("Lozinka ID: {} ažurirana od strane: {}", id, adminEmail);

        return mapToResponse(updated);
    }

    // Briše lozinku
    public void deletePassword(Long id, String adminEmail) {
        PasswordEntry entry = passwordEntryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lozinka nije pronađena sa ID: " + id));

        // Sigurnosna provera
        if (!entry.getOwner().getEmail().equals(adminEmail)) {
            log.warn("Admin {} pokušao obrisati tuđu lozinku ID: {}", adminEmail, id);
            throw new RuntimeException("Nemate dozvolu za brisanje ove lozinke");
        }

        passwordEntryRepository.delete(entry);
        log.info("Lozinka ID: {} obrisana od strane: {}", id, adminEmail);
    }

    // Mapira PasswordEntry model na PasswordEntryResponse DTO
    private PasswordEntryResponse mapToResponse(PasswordEntry entry) {
        return new PasswordEntryResponse(
            entry.getId(),
            entry.getSiteName(),
            entry.getUsername(),
            entry.getEncryptedPassword(),
            entry.getCreatedAt(),
            entry.getUpdatedAt()
        );
    }
}