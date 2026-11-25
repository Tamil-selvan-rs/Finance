package com.myfinance.repository;

import com.myfinance.entity.CredentialVault;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CredentialVaultRepository extends JpaRepository<CredentialVault, Long> {
    List<CredentialVault> findByUserIdOrderByAppTitleAsc(Long userId);
    Optional<CredentialVault> findByIdAndUserId(Long id, Long userId);
}
