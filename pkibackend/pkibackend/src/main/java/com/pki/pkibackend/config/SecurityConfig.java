package com.pki.pkibackend.config;

import com.pki.pkibackend.security.JwtAuthFilter;
import com.pki.pkibackend.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Omogućava @PreAuthorize anotacije na kontrolerima
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Isključujemo CSRF jer koristimo JWT (ne koristimo cookies)
            .csrf(csrf -> csrf.disable())

            // Podešavamo CORS da Angular može da šalje requestove
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Definišemo koje rute su javne a koje zaštićene
            .authorizeHttpRequests(auth -> auth
                // Javne rute — ne treba token
                .requestMatchers("/api/auth/**").permitAll()
                // Sve ostale rute zahtevaju autentifikaciju
                .anyRequest().authenticated()
            )

            // Koristimo STATELESS session — ne čuvamo session na serveru
            // Svaki request mora imati JWT token
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Dodajemo naš AuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // Dodajemo JWT filter PRZED standardnim Spring Security filterom
            // Znači svaki request prvo prolazi kroz naš JWT filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS konfiguracija — dozvoljava Angular (localhost:4200) da komunicira sa backendom
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Dozvoljeni origins — Angular development server
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));

        // Dozvoljene HTTP metode
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Dozvoljeni headeri — važno da uključimo Authorization za JWT
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // Dozvoljavamo slanje kredencijala (cookies, auth headeri)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Primenjujemo ovu konfiguraciju na sve rute
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // AuthenticationProvider — koristi našu bazu za proveru korisnika
    // i BCrypt za proveru lozinke
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // BCrypt enkoder za lozinke
    // Automatski soli i hashuje lozinku
    // Strength 10 je dobar balans između sigurnosti i performansi
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // AuthenticationManager se koristi u AuthService za login
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}