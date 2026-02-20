package com.pki.pkibackend.config;

import com.pki.pkibackend.model.User;
import com.pki.pkibackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // CommandLineRunner se izvršava automatski pri pokretanju aplikacije
    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // Proveravamo da li admin već postoji da ne bismo kreirali duplikata
            if (!userRepository.existsByEmail("admin@pki.com")) {
                User admin = new User();
                admin.setEmail("admin@pki.com");
                // Lozinka se hashuje BCryptom pre čuvanja u bazu
                admin.setPassword(passwordEncoder.encode("Admin123!"));
                admin.setFirstName("Admin");
                admin.setLastName("PKI");
                admin.setRole(User.Role.ADMIN);

                userRepository.save(admin);
                log.info("Admin korisnik kreiran: admin@pki.com / Admin123!");
            } else {
                log.info("Admin korisnik već postoji, preskačem kreiranje");
            }
        };
    }
}
