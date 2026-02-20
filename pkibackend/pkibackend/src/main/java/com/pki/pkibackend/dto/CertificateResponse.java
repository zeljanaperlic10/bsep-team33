package com.pki.pkibackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CertificateResponse {

    private Long id;
    private String alias;
    private String commonName;
    private String organization;
    private String organizationalUnit;
    private String country;
    private String serialNumber;
    private String type;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String issuerAlias;
    private boolean revoked;
    private String revocationReason;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;

    public CertificateResponse() {}

    public CertificateResponse(Long id, String alias, String commonName,
                                String organization, String organizationalUnit,
                                String country, String serialNumber, String type,
                                LocalDateTime validFrom, LocalDateTime validTo,
                                String issuerAlias, boolean revoked,
                                String revocationReason, LocalDateTime revokedAt,
                                LocalDateTime createdAt) {
        this.id = id;
        this.alias = alias;
        this.commonName = commonName;
        this.organization = organization;
        this.organizationalUnit = organizationalUnit;
        this.country = country;
        this.serialNumber = serialNumber;
        this.type = type;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.issuerAlias = issuerAlias;
        this.revoked = revoked;
        this.revocationReason = revocationReason;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getOrganizationalUnit() { return organizationalUnit; }
    public void setOrganizationalUnit(String ou) { this.organizationalUnit = ou; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }

    public String getIssuerAlias() { return issuerAlias; }
    public void setIssuerAlias(String issuerAlias) { this.issuerAlias = issuerAlias; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}