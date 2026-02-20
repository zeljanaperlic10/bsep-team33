package com.pki.pkibackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class CertificateRequest {

    // X500Name podaci o vlasniku sertifikata
    @NotBlank(message = "Common Name je obavezan")
    private String commonName;

    @NotBlank(message = "Organizacija je obavezna")
    private String organization;

    private String organizationalUnit;

    private String country;

    private String state;

    private String locality;

    // Tip sertifikata: ROOT, INTERMEDIATE ili END_ENTITY
    @NotNull(message = "Tip sertifikata je obavezan")
    private String type;

    // Datum od kad važi sertifikat
    @NotNull(message = "Datum početka važenja je obavezan")
    private LocalDateTime validFrom;

    // Datum do kad važi sertifikat
    @NotNull(message = "Datum kraja važenja je obavezan")
    private LocalDateTime validTo;

    // Alias CA sertifikata koji potpisuje novi sertifikat
    // Za ROOT sertifikat ovo je null jer je samopotpisan
    private String issuerAlias;

    // Ekstenzije sertifikata
    // Npr: ["keyCertSign", "cRLSign"] za CA sertifikate
    private List<String> keyUsages;

    // Npr: ["serverAuth", "clientAuth"] za EE sertifikate
    private List<String> extendedKeyUsages;

    // Da li je CA sertifikat (može potpisivati druge sertifikate)
    private boolean isCA;

    public CertificateRequest() {}

    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getOrganizationalUnit() { return organizationalUnit; }
    public void setOrganizationalUnit(String organizationalUnit) { this.organizationalUnit = organizationalUnit; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }

    public String getIssuerAlias() { return issuerAlias; }
    public void setIssuerAlias(String issuerAlias) { this.issuerAlias = issuerAlias; }

    public List<String> getKeyUsages() { return keyUsages; }
    public void setKeyUsages(List<String> keyUsages) { this.keyUsages = keyUsages; }

    public List<String> getExtendedKeyUsages() { return extendedKeyUsages; }
    public void setExtendedKeyUsages(List<String> extendedKeyUsages) { this.extendedKeyUsages = extendedKeyUsages; }

    public boolean isCA() { return isCA; }
    public void setCA(boolean CA) { isCA = CA; }
}