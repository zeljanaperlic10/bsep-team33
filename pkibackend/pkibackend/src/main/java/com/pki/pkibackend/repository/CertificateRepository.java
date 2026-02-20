package com.pki.pkibackend.repository;

import com.pki.pkibackend.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByAlias(String alias);
    List<Certificate> findByType(Certificate.CertificateType type);
    List<Certificate> findByRevokedFalse();
    List<Certificate> findByTypeAndRevokedFalse(Certificate.CertificateType type);
    boolean existsByAlias(String alias);
}