package com.pki.pkibackend.service;

import com.pki.pkibackend.model.User;
import com.pki.pkibackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class KeyPairService {

    private static final Logger log = LoggerFactory.getLogger(KeyPairService.class);

    private final UserRepository userRepository;

    public KeyPairService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Generiše novi RSA par ključeva za admina
    // Javni ključ se čuva u bazi
    // Privatni ključ se vraća adminu JEDNOM i nikad se ne čuva
    // Ako admin izgubi privatni ključ, mora generisati novi par
    public Map<String, String> generateKeyPair(String adminEmail) throws Exception {
        // Učitavamo admina
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Admin nije pronađen: " + adminEmail));

        // Generišemo RSA par ključeva od 2048 bita
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Enkodujemo ključeve u Base64 format za čuvanje i prenos
        String publicKeyBase64 = Base64.getEncoder()
            .encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder()
            .encodeToString(keyPair.getPrivate().getEncoded());

        // Čuvamo SAMO javni ključ u bazi
        admin.setPublicKey(publicKeyBase64);
        userRepository.save(admin);

        log.info("Novi par ključeva generisan za admina: {}", adminEmail);
        log.warn("Privatni ključ vraćen adminu — neće biti sačuvan na serveru");

        // Vraćamo oba ključa — frontend mora sačuvati privatni ključ lokalno
        // Nakon ovog poziva privatni ključ više nije dostupan sa servera
        Map<String, String> result = new HashMap<>();
        result.put("publicKey", publicKeyBase64);
        result.put("privateKey", privateKeyBase64);
        return result;
    }

    // Vraća javni ključ admina
    // Koristi se kada frontend treba da enkriptuje lozinku
    public String getPublicKey(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Admin nije pronađen: " + adminEmail));

        if (admin.getPublicKey() == null) {
            throw new RuntimeException(
                "Admin nema generisan par ključeva. " +
                "Molimo generišite par ključeva pre korišćenja password managera."
            );
        }

        return admin.getPublicKey();
    }

    // Proverava da li admin ima generisan par ključeva
    public boolean hasKeyPair(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Admin nije pronađen: " + adminEmail));

        return admin.getPublicKey() != null;
    }
}