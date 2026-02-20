package com.pki.pkibackend.repository;

import com.pki.pkibackend.model.PasswordEntry;
import com.pki.pkibackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {
    List<PasswordEntry> findByOwner(User owner);
    List<PasswordEntry> findByOwnerId(Long ownerId);
}