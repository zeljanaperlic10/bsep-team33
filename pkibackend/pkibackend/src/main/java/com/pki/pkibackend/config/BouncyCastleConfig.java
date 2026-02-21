package com.pki.pkibackend.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.security.Security;

@Configuration
public class BouncyCastleConfig {

    private static final Logger log = LoggerFactory.getLogger(BouncyCastleConfig.class);

    // @PostConstruct se izvršava automatski pri pokretanju aplikacije
    // Registrujemo Bouncy Castle kao security provider
    // Mora se uraditi pre bilo kakve kriptografske operacije
    @PostConstruct
    public void registerBouncyCastle() {
        // Proveravamo da nije već registrovan da ne bismo dodali duplikat
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("Bouncy Castle provider uspešno registrovan");
        } else {
            log.info("Bouncy Castle provider već registrovan");
        }
    }
}