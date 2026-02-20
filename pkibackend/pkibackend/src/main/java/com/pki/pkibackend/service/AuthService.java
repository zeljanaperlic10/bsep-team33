package com.pki.pkibackend.service;

import com.pki.pkibackend.dto.LoginRequest;
import com.pki.pkibackend.dto.LoginResponse;
import com.pki.pkibackend.model.User;
import com.pki.pkibackend.repository.UserRepository;
import com.pki.pkibackend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            // Korak 1: Spring Security proverava email i lozinku
            // Ako su pogrešni baca AuthenticationException
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            // Korak 2: Učitaj korisnika iz baze
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));

            // Korak 3: Generiši JWT token sa emailom i rolom
            String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getRole().name()
            );

            // Korak 4: Logiraj uspešnu prijavu
            log.info("Uspešna prijava korisnika: {}", request.getEmail());

            // Korak 5: Vrati token i podatke o korisniku
            return new LoginResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
            );

        } catch (AuthenticationException e) {
            // Logiraj neuspešnu prijavu — važno za audit log
            log.warn("Neuspešna prijava za korisnika: {}", request.getEmail());
            throw new RuntimeException("Pogrešan email ili lozinka");
        }
    }
}