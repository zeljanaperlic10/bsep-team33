package com.pki.pkibackend.service;

import com.pki.pkibackend.dto.CertificateRequest;
import com.pki.pkibackend.dto.CertificateResponse;
import com.pki.pkibackend.model.Certificate;
import com.pki.pkibackend.repository.CertificateRepository;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final KeystoreService keystoreService;

    public CertificateService(CertificateRepository certificateRepository,
                               KeystoreService keystoreService) {
        this.certificateRepository = certificateRepository;
        this.keystoreService = keystoreService;
    }

    // Glavni metod koji se poziva iz kontrolera
    // Na osnovu tipa sertifikata poziva odgovarajući metod
    public CertificateResponse generateCertificate(CertificateRequest request) throws Exception {
        switch (request.getType().toUpperCase()) {
            case "ROOT":
                return generateRootCertificate(request);
            case "INTERMEDIATE":
                return generateIntermediateCertificate(request);
            case "END_ENTITY":
                return generateEndEntityCertificate(request);
            default:
                throw new IllegalArgumentException("Nepoznat tip sertifikata: " + request.getType());
        }
    }

    // ================================================================
    // ROOT sertifikat — samopotpisan, vrh lanca
    // ================================================================
    private CertificateResponse generateRootCertificate(CertificateRequest request) throws Exception {
        log.info("Generisanje ROOT sertifikata za: {}", request.getCommonName());

        // Korak 1: Generišemo par ključeva
        KeyPair keyPair = generateKeyPair();

        // Korak 2: Kreiramo X500Name
        X500Name subjectName = buildX500Name(request);

        // Korak 3: Serijski broj
        BigInteger serialNumber = new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16);

        // Korak 4: Datumi važenja
        Date validFrom = Date.from(request.getValidFrom().atZone(ZoneId.systemDefault()).toInstant());
        Date validTo = Date.from(request.getValidTo().atZone(ZoneId.systemDefault()).toInstant());

        // Korak 5: Gradimo sertifikat — za ROOT issuer == subject
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            subjectName,
            serialNumber,
            validFrom,
            validTo,
            subjectName,
            keyPair.getPublic()
        );

        // Korak 6: Ekstenzije
        // BasicConstraints(true) — označava da je ovo CA sertifikat
        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            new BasicConstraints(true)
        );

        // KeyUsage — može potpisivati sertifikate i CRL
        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

        // SubjectKeyIdentifier
        certBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false,
            createSubjectKeyIdentifier(keyPair.getPublic())
        );

        // Korak 7: Potpisujemo sopstvenim privatnim ključem
        X509Certificate certificate = signCertificate(certBuilder, keyPair.getPrivate());

        // Korak 8: Alias za keystore
        String alias = "root-" + request.getCommonName().toLowerCase().replace(" ", "-")
                + "-" + System.currentTimeMillis();

        // Korak 9: Čuvamo u keystore SA privatnim ključem
        keystoreService.saveCAcertificate(
            alias,
            keyPair.getPrivate(),
            new X509Certificate[]{certificate}
        );

        // Korak 10: Čuvamo u bazu
        Certificate cert = saveCertificateToDatabase(request, alias, serialNumber.toString(), null);

        log.info("ROOT sertifikat uspešno kreiran sa aliasom: {}", alias);
        return mapToResponse(cert);
    }

    // ================================================================
    // INTERMEDIATE sertifikat — potpisan od ROOT ili drugog INTERMEDIATE
    // ================================================================
    private CertificateResponse generateIntermediateCertificate(CertificateRequest request) throws Exception {
        log.info("Generisanje INTERMEDIATE sertifikata za: {}", request.getCommonName());

        // Korak 1: Validiramo issuer
        validateIssuer(request.getIssuerAlias());

        // Korak 2: Učitavamo issuer iz keystore
        X509Certificate issuerCert = keystoreService.getCertificate(request.getIssuerAlias());
        PrivateKey issuerPrivateKey = keystoreService.getPrivateKey(request.getIssuerAlias());

        // Korak 3: Novi par ključeva za intermediate
        KeyPair keyPair = generateKeyPair();

        X500Name issuerName = new X500Name(issuerCert.getSubjectX500Principal().getName());
        X500Name subjectName = buildX500Name(request);

        BigInteger serialNumber = new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16);
        Date validFrom = Date.from(request.getValidFrom().atZone(ZoneId.systemDefault()).toInstant());
        Date validTo = Date.from(request.getValidTo().atZone(ZoneId.systemDefault()).toInstant());

        // Korak 4: Proveravamo da ne traje duže od issuera
        if (validTo.after(issuerCert.getNotAfter())) {
            throw new IllegalArgumentException(
                "Datum isteka ne može biti posle datuma isteka issuer sertifikata: "
                + issuerCert.getNotAfter()
            );
        }

        // Korak 5: Gradimo sertifikat
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            validFrom,
            validTo,
            subjectName,
            keyPair.getPublic()
        );

        // Korak 6: Ekstenzije
        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            new BasicConstraints(true)
        );

        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

        certBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false,
            createSubjectKeyIdentifier(keyPair.getPublic())
        );

        // Korak 7: Potpisujemo issuerovim privatnim ključem
        X509Certificate certificate = signCertificate(certBuilder, issuerPrivateKey);

        // Korak 8: Gradimo kompletan lanac
        X509Certificate[] issuerChain = keystoreService.getCertificateChain(request.getIssuerAlias());
        X509Certificate[] fullChain = buildCertificateChain(certificate, issuerChain);

        // Korak 9: Čuvamo u keystore
        String alias = "intermediate-" + request.getCommonName().toLowerCase().replace(" ", "-")
                + "-" + System.currentTimeMillis();

        keystoreService.saveCAcertificate(alias, keyPair.getPrivate(), fullChain);

        // Korak 10: Čuvamo u bazu
        Certificate cert = saveCertificateToDatabase(
            request, alias, serialNumber.toString(), request.getIssuerAlias()
        );

        log.info("INTERMEDIATE sertifikat uspešno kreiran sa aliasom: {}", alias);
        return mapToResponse(cert);
    }

    // ================================================================
    // END ENTITY sertifikat — krajnji korisnik, ne može potpisivati
    // ================================================================
    private CertificateResponse generateEndEntityCertificate(CertificateRequest request) throws Exception {
        log.info("Generisanje END_ENTITY sertifikata za: {}", request.getCommonName());

        // Korak 1: Validiramo issuer
        validateIssuer(request.getIssuerAlias());

        // Korak 2: Učitavamo issuer
        X509Certificate issuerCert = keystoreService.getCertificate(request.getIssuerAlias());
        PrivateKey issuerPrivateKey = keystoreService.getPrivateKey(request.getIssuerAlias());

        // Korak 3: Novi par ključeva za EE
        KeyPair keyPair = generateKeyPair();

        X500Name issuerName = new X500Name(issuerCert.getSubjectX500Principal().getName());
        X500Name subjectName = buildX500Name(request);

        BigInteger serialNumber = new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16);
        Date validFrom = Date.from(request.getValidFrom().atZone(ZoneId.systemDefault()).toInstant());
        Date validTo = Date.from(request.getValidTo().atZone(ZoneId.systemDefault()).toInstant());

        // Provera trajanja
        if (validTo.after(issuerCert.getNotAfter())) {
            throw new IllegalArgumentException(
                "Datum isteka ne može biti posle datuma isteka issuer sertifikata: "
                + issuerCert.getNotAfter()
            );
        }

        // Korak 4: Gradimo sertifikat
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            validFrom,
            validTo,
            subjectName,
            keyPair.getPublic()
        );

        // Korak 5: Ekstenzije za EE
        // BasicConstraints(false) — EE NIJE CA
        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            new BasicConstraints(false)
        );

        // KeyUsage za EE
        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );

        // ExtendedKeyUsage — koristimo OID vrednosti direktno
        // 1.3.6.1.5.5.7.3.1 = serverAuth
        // 1.3.6.1.5.5.7.3.2 = clientAuth
        certBuilder.addExtension(
            Extension.extendedKeyUsage,
            false,
            new ExtendedKeyUsage(new KeyPurposeId[]{
                KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.3.6.1.5.5.7.3.1")),
                KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.3.6.1.5.5.7.3.2"))
            })
        );

        certBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false,
            createSubjectKeyIdentifier(keyPair.getPublic())
        );

        // Korak 6: Potpisujemo issuerovim privatnim ključem
        X509Certificate certificate = signCertificate(certBuilder, issuerPrivateKey);

        // Korak 7: EE čuvamo BEZ privatnog ključa
        String alias = "ee-" + request.getCommonName().toLowerCase().replace(" ", "-")
                + "-" + System.currentTimeMillis();

        keystoreService.saveEEcertificate(alias, certificate);

        // Korak 8: Čuvamo u bazu
        Certificate cert = saveCertificateToDatabase(
            request, alias, serialNumber.toString(), request.getIssuerAlias()
        );

        log.info("END_ENTITY sertifikat uspešno kreiran sa aliasom: {}", alias);
        return mapToResponse(cert);
    }

    // ================================================================
    // Helper metodi
    // ================================================================

    // Generiše RSA par ključeva od 2048 bita
    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    // Gradi X500Name iz podataka requesta
    private X500Name buildX500Name(CertificateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("CN=").append(request.getCommonName());

        if (request.getOrganization() != null && !request.getOrganization().isEmpty()) {
            sb.append(", O=").append(request.getOrganization());
        }
        if (request.getOrganizationalUnit() != null && !request.getOrganizationalUnit().isEmpty()) {
            sb.append(", OU=").append(request.getOrganizationalUnit());
        }
        if (request.getCountry() != null && !request.getCountry().isEmpty()) {
            sb.append(", C=").append(request.getCountry());
        }
        if (request.getState() != null && !request.getState().isEmpty()) {
            sb.append(", ST=").append(request.getState());
        }
        if (request.getLocality() != null && !request.getLocality().isEmpty()) {
            sb.append(", L=").append(request.getLocality());
        }

        return new X500Name(sb.toString());
    }

    // Potpisuje sertifikat datim privatnim ključem
    private X509Certificate signCertificate(X509v3CertificateBuilder certBuilder,
                                             PrivateKey privateKey) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(privateKey);

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder);
    }

    // Kreira SubjectKeyIdentifier iz javnog ključa
    private SubjectKeyIdentifier createSubjectKeyIdentifier(PublicKey publicKey) throws Exception {
        byte[] publicKeyBytes = publicKey.getEncoded();
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(publicKeyBytes);
        return new SubjectKeyIdentifier(digest);
    }

    // Gradi kompletan lanac sertifikata
    private X509Certificate[] buildCertificateChain(X509Certificate newCert,
                                                      X509Certificate[] issuerChain) {
        X509Certificate[] chain = new X509Certificate[issuerChain.length + 1];
        chain[0] = newCert;
        System.arraycopy(issuerChain, 0, chain, 1, issuerChain.length);
        return chain;
    }

    // Validira issuer pre potpisivanja
    private void validateIssuer(String issuerAlias) throws Exception {
        if (issuerAlias == null || issuerAlias.isEmpty()) {
            throw new IllegalArgumentException("Issuer alias je obavezan");
        }

        Certificate issuerInDb = certificateRepository.findByAlias(issuerAlias)
            .orElseThrow(() -> new Exception("Issuer sertifikat nije pronađen: " + issuerAlias));

        if (issuerInDb.isRevoked()) {
            throw new IllegalArgumentException(
                "Issuer sertifikat je povučen i ne može se koristiti za potpisivanje"
            );
        }

        X509Certificate issuerCert = keystoreService.getCertificate(issuerAlias);
        if (issuerCert.getNotAfter().before(new Date())) {
            throw new IllegalArgumentException("Issuer sertifikat je istekao");
        }
    }

    // Čuva metapodatke sertifikata u bazu
    private Certificate saveCertificateToDatabase(CertificateRequest request,
                                                    String alias,
                                                    String serialNumber,
                                                    String issuerAlias) {
        Certificate cert = new Certificate();
        cert.setAlias(alias);
        cert.setCommonName(request.getCommonName());
        cert.setOrganization(request.getOrganization());
        cert.setOrganizationalUnit(request.getOrganizationalUnit());
        cert.setCountry(request.getCountry());
        cert.setSerialNumber(serialNumber);
        cert.setType(Certificate.CertificateType.valueOf(request.getType().toUpperCase()));
        cert.setValidFrom(request.getValidFrom());
        cert.setValidTo(request.getValidTo());
        cert.setIssuerAlias(issuerAlias);
        cert.setRevoked(false);

        return certificateRepository.save(cert);
    }

    // ================================================================
    // Javni metodi za kontroler
    // ================================================================

    // Vraća sve sertifikate
    public List<CertificateResponse> getAllCertificates() {
        List<Certificate> certificates = certificateRepository.findAll();
        List<CertificateResponse> responses = new ArrayList<>();
        for (Certificate cert : certificates) {
            responses.add(mapToResponse(cert));
        }
        return responses;
    }

    // Vraća jedan sertifikat po ID-u
    public CertificateResponse getCertificateById(Long id) {
        Certificate cert = certificateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sertifikat nije pronađen sa ID: " + id));
        return mapToResponse(cert);
    }

    // Vraća sve CA sertifikate koji nisu povučeni
    // Koristi se za dropdown na frontendu kada biramo issuera
    public List<CertificateResponse> getAvailableIssuers() {
        List<Certificate> issuers = new ArrayList<>();
        issuers.addAll(certificateRepository.findByTypeAndRevokedFalse(
            Certificate.CertificateType.ROOT));
        issuers.addAll(certificateRepository.findByTypeAndRevokedFalse(
            Certificate.CertificateType.INTERMEDIATE));

        List<CertificateResponse> responses = new ArrayList<>();
        for (Certificate cert : issuers) {
            responses.add(mapToResponse(cert));
        }
        return responses;
    }

    // Povlačenje sertifikata
    public CertificateResponse revokeCertificate(Long id, String reason) {
        Certificate cert = certificateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sertifikat nije pronađen"));

        if (cert.isRevoked()) {
            throw new IllegalStateException("Sertifikat je već povučen");
        }

        cert.setRevoked(true);
        cert.setRevocationReason(reason);
        cert.setRevokedAt(java.time.LocalDateTime.now());

        certificateRepository.save(cert);
        log.warn("Sertifikat povučen: {} — razlog: {}", cert.getAlias(), reason);

        return mapToResponse(cert);
    }

    // Mapira Certificate model na CertificateResponse DTO
    private CertificateResponse mapToResponse(Certificate cert) {
        return new CertificateResponse(
            cert.getId(),
            cert.getAlias(),
            cert.getCommonName(),
            cert.getOrganization(),
            cert.getOrganizationalUnit(),
            cert.getCountry(),
            cert.getSerialNumber(),
            cert.getType().name(),
            cert.getValidFrom(),
            cert.getValidTo(),
            cert.getIssuerAlias(),
            cert.isRevoked(),
            cert.getRevocationReason(),
            cert.getRevokedAt(),
            cert.getCreatedAt()
        );
    }
}