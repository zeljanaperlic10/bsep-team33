package com.pki.pkibackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Service
public class KeystoreService {

    private static final Logger log = LoggerFactory.getLogger(KeystoreService.class);

    @Value("${keystore.password}")
    private String keystorePassword;

    @Value("${keystore.path}")
    private String keystorePath;

    private static final String KEYSTORE_FILE = "pki-keystore.jks";
    private static final String KEYSTORE_TYPE = "JKS";

    private String getKeystoreFilePath() {
        return keystorePath + KEYSTORE_FILE;
    }

    private KeyStore loadOrCreateKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

        // Kreiramo folder ako ne postoji
        File keystoreDir = new File(keystorePath);
        if (!keystoreDir.exists()) {
            boolean created = keystoreDir.mkdirs();
            log.info("Keystore folder kreiran: {} — {}", keystorePath, created);
        }

        File keystoreFile = new File(getKeystoreFilePath());

        if (keystoreFile.exists()) {
            // Proveravamo da fajl nije prazan
            if (keystoreFile.length() == 0) {
                log.warn("Keystore fajl je prazan — brišemo i kreiramo novi");
                keystoreFile.delete();
                keyStore.load(null, keystorePassword.toCharArray());
            } else {
                try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                    keyStore.load(fis, keystorePassword.toCharArray());
                    log.debug("Keystore učitan: {}", getKeystoreFilePath());
                } catch (IOException e) {
                    log.warn("Keystore fajl je oštećen — brišemo i kreiramo novi: {}", e.getMessage());
                    keystoreFile.delete();
                    keyStore.load(null, keystorePassword.toCharArray());
                }
            }
        } else {
            keyStore.load(null, keystorePassword.toCharArray());
            log.info("Novi keystore kreiran: {}", getKeystoreFilePath());
        }

        return keyStore;
    }

    private void saveKeystore(KeyStore keyStore) throws Exception {
        File keystoreDir = new File(keystorePath);
        if (!keystoreDir.exists()) {
            keystoreDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(getKeystoreFilePath())) {
            keyStore.store(fos, keystorePassword.toCharArray());
            log.debug("Keystore sačuvan: {}", getKeystoreFilePath());
        }
    }

    // Čuva CA sertifikat SA privatnim ključem
    public void saveCAcertificate(String alias, PrivateKey privateKey,
                                   X509Certificate[] certificateChain) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        keyStore.setKeyEntry(
            alias,
            privateKey,
            keystorePassword.toCharArray(),
            certificateChain
        );
        saveKeystore(keyStore);
        log.info("CA sertifikat sačuvan sa aliasom: {}", alias);
    }

    // Čuva EE sertifikat BEZ privatnog ključa
    public void saveEEcertificate(String alias, X509Certificate certificate) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        keyStore.setCertificateEntry(alias, certificate);
        saveKeystore(keyStore);
        log.info("EE sertifikat sačuvan sa aliasom: {}", alias);
    }

    // Čita privatni ključ iz keystore
    public PrivateKey getPrivateKey(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        Key key = keyStore.getKey(alias, keystorePassword.toCharArray());
        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }
        throw new Exception("Privatni ključ nije pronađen za alias: " + alias);
    }

    // Čita sertifikat iz keystore
    public X509Certificate getCertificate(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        Certificate cert = keyStore.getCertificate(alias);
        if (cert instanceof X509Certificate) {
            return (X509Certificate) cert;
        }
        throw new Exception("Sertifikat nije pronađen za alias: " + alias);
    }

    // Čita lanac sertifikata
    public X509Certificate[] getCertificateChain(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        Certificate[] chain = keyStore.getCertificateChain(alias);

        if (chain == null) {
            X509Certificate cert = getCertificate(alias);
            return new X509Certificate[]{cert};
        }

        X509Certificate[] x509Chain = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            x509Chain[i] = (X509Certificate) chain[i];
        }
        return x509Chain;
    }

    public boolean aliasExists(String alias) throws Exception {
        KeyStore keyStore = loadOrCreateKeystore();
        return keyStore.containsAlias(alias);
    }
}