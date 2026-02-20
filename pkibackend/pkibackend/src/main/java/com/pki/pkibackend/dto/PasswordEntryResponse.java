package com.pki.pkibackend.dto;

import java.time.LocalDateTime;

public class PasswordEntryResponse {

    private Long id;
    private String siteName;
    private String username;

    // Šaljemo enkriptovanu lozinku — dekripcija je na frontendu
    // Privatni ključ nikad ne dolazi na server
    private String encryptedPassword;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PasswordEntryResponse() {}

    public PasswordEntryResponse(Long id, String siteName, String username,
                                  String encryptedPassword, LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        this.id = id;
        this.siteName = siteName;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String ep) { this.encryptedPassword = ep; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}