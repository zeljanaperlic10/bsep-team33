package com.pki.pkibackend.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordEntryRequest {

    @NotBlank(message = "Naziv sajta je obavezan")
    private String siteName;

    @NotBlank(message = "Korisničko ime je obavezno")
    private String username;

    // Lozinka dolazi već enkriptovana sa frontenda
    // Frontend enkriptuje pomoću Web Crypto API i admin javnog ključa
    @NotBlank(message = "Enkriptovana lozinka je obavezna")
    private String encryptedPassword;

    public PasswordEntryRequest() {}

    public PasswordEntryRequest(String siteName, String username, String encryptedPassword) {
        this.siteName = siteName;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
    }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }
}