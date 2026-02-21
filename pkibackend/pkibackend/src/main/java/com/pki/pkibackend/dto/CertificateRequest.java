package com.pki.pkibackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class CertificateRequest {

    private String commonName;
    private String organization;
    private String organizationalUnit;
    private String country;
    private String state;
    private String locality;
    private String type;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validTo;

    private String issuerAlias;
    private List<String> keyUsages;
    private List<String> extendedKeyUsages;

    // Ovo je kljucno — bez ovoga Jackson čita kao "CA" umesto "isCA"
    @JsonProperty("isCA")
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

    @JsonProperty("isCA")
    public boolean isCA() { return isCA; }

    @JsonProperty("isCA")
    public void setCA(boolean CA) { isCA = CA; }
}