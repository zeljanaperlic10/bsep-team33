package com.pki.pkibackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class KeystoreService {

    private static final Logger log = LoggerFactory.getLogger(KeystoreService.class);

    // Lozinka za keystore — čitamo iz application.properties
    @Value("${keystore.password}")
    private String keystorePassword;

    // Putanja do keystore foldera
    @Value("${keystore.path}")
    private String keystorePath;

    // Naziv keystore fajla
    private static final String KEYSTORE_FILE = "pki-keystore.jks";

    // Tip keystore-a
    private static final String KEYSTORE_TYPE = "JKS";

    // Vraća punu putanju do keystore fajla
    private String getKeystoreFilePath() {
        return keystorePath + KEYSTORE_FILE;
    }

    // Učitava postojeći keystore ili kreira novi ako ne postoji
    private KeyStore loadOrCreateKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        File keystoreFile = new File(getKeystoreFilePath());

        if (keystoreFile.exists()) {
            // Učitavamo postojeći keystore
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keyStore.load(fis, keystorePassword.toCharArray());
                log.debug("Keystore učitan: {}", getKeystoreFilePath());
            }
        } else {
            // Kreiramo novi prazan keystore
            // Kreiramo folder ako ne postoji
            Files.createDirectories(Paths.get(keystorePath));
            keyStore.load(null, keystorePassword.toCharArray());
            log.info("Novi keystore kreiran: {}", getKeystoreFilePath());
        }

        return keyStore;
    }

    // Čuva keystore na disk
    private void saveKeystore(KeyStore keyStore) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(getKeystoreFilePath())) {
            keyStore.store(fos, keystorePassword.toCharArray());
            log.debug("Keystore sačuvan na disk");
        }
    }

    // Čuva CA sertifikat SA privatnim ključem u keystore
    // Koristi se za ROOT i INTERMEDIATE sertifikate
    public void saveCAcertificate(String alias, PrivateKey privateKey,
                                   X509Certificate[] certificateChain) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();

        // PrivateKeyEntry čuva privatni ključ zajedno sa lancem sertifikata
        keyStore.setKeyEntry(
            alias,
            privateKey,
            keystorePassword.toCharArray(),
            certificateChain
        );

        saveKeystore(keyStore);
        log.info("CA sertifikat sačuvan u keystore sa aliasom: {}", alias);
    }

    // Čuva EE sertifikat BEZ privatnog ključa u keystore
    // Privatni ključ EE sertifikata se ne sme čuvati na serveru
    public void saveEEcertificate(String alias, X509Certificate certificate) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();

        // TrustedCertificateEntry čuva samo javni sertifikat bez privatnog ključa
        keyStore.setCertificateEntry(alias, certificate);

        saveKeystore(keyStore);
        log.info("EE sertifikat sačuvan u keystore sa aliasom: {}", alias);
    }

    // Čita privatni ključ iz keystore po alisu
    // Koristi se kada treba potpisati novi sertifikat
    public PrivateKey getPrivateKey(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        Key key = keyStore.getKey(alias, keystorePassword.toCharArray());

        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }

        throw new Exception("Privatni ključ nije pronađen za alias: " + alias);
    }

    // Čita sertifikat iz keystore po alisu
    public X509Certificate getCertificate(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        Certificate cert = keyStore.getCertificate(alias);

        if (cert instanceof X509Certificate) {
            return (X509Certificate) cert;
        }

        throw new Exception("Sertifikat nije pronađen za alias: " + alias);
    }

    // Čita kompletan lanac sertifikata iz keystore po alisu
    // Koristi se kada treba izgraditi lanac za novi sertifikat
    public X509Certificate[] getCertificateChain(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        Certificate[] chain = keyStore.getCertificateChain(alias);

        if (chain == null) {
            // Ako nema lanca vraćamo samo sertifikat kao niz od jednog elementa
            X509Certificate cert = getCertificate(alias);
            return new X509Certificate[]{cert};
        }

        X509Certificate[] x509Chain = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            x509Chain[i] = (X509Certificate) chain[i];
        }

        return x509Chain;
    }

    // Proverava da li alias već postoji u keystoru
    public boolean aliasExists(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        return keyStore.containsAlias(alias);
    }
}