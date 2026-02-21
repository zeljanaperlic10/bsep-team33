package com.pki.pkibackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice znači da ovaj fajl hendluje greške za sve kontrolere
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Pomoćni metod koji kreira standardni format odgovora za grešku
    // Svaka greška ima: timestamp, status, message
    private Map<String, Object> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status.value());
        error.put("message", message);
        return error;
    }

    // Hvata grešku kada korisnik pošalje pogrešan email ili lozinku
    // Vraća 401 Unauthorized
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        log.warn("Neuspešna autentifikacija: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(buildErrorResponse(HttpStatus.UNAUTHORIZED, "Pogrešan email ili lozinka"));
    }

    // Hvata grešku kada korisnik nema dozvolu za pristup resursu
    // Npr. pokušava da pristupi admin endpointu bez admin role
    // Vraća 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {
        log.warn("Pristup odbijen: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(buildErrorResponse(HttpStatus.FORBIDDEN, "Nemate dozvolu za ovu akciju"));
    }

    // Hvata grešku validacije — kada @Valid u kontroleru odbije request
    // Npr. prazan email, kratka lozinka itd.
    // Vraća 400 Bad Request sa listom grešaka po poljima
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Prikupljamo sve greške validacije po poljima
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = buildErrorResponse(
            HttpStatus.BAD_REQUEST, "Validacija nije uspešna"
        );
        response.put("errors", fieldErrors);

        log.warn("Greška validacije: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Hvata IllegalArgumentException — npr. pogrešan tip sertifikata
    // ili datum isteka posle datuma issuera
    // Vraća 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("Neispravan argument: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    // Hvata IllegalStateException — npr. sertifikat je već povučen
    // Vraća 409 Conflict
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex) {
        log.warn("Neispravan status: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // Hvata RuntimeException — npr. korisnik ili sertifikat nije pronađen
    // Vraća 404 Not Found
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {
        log.error("Runtime greška: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // Hvata sve ostale greške koje nisu pokrivene gore
    // Vraća 500 Internal Server Error
    // Ne pokazujemo detalje greške korisniku iz bezbednosnih razloga
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Neočekivana greška: {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Došlo je do interne greške servera"
            ));
    }
}
