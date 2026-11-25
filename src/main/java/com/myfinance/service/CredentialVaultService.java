package com.myfinance.service;

import com.myfinance.dto.CredentialVaultRequest;
import com.myfinance.dto.CredentialVaultResponse;
import com.myfinance.entity.CredentialVault;
import com.myfinance.entity.User;
import com.myfinance.exception.ResourceNotFoundException;
import com.myfinance.repository.CredentialVaultRepository;
import com.myfinance.repository.UserRepository;
import com.myfinance.security.VaultCryptoService;
import com.myfinance.validator.CredentialVaultValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CredentialVaultService {

    private final CredentialVaultRepository vaultRepository;
    private final UserRepository userRepository;
    private final CredentialVaultValidator validator;
    private final VaultCryptoService cryptoService;

    public CredentialVaultService(CredentialVaultRepository vaultRepository,
                                  UserRepository userRepository,
                                  CredentialVaultValidator validator,
                                  VaultCryptoService cryptoService) {
        this.vaultRepository = vaultRepository;
        this.userRepository = userRepository;
        this.validator = validator;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public CredentialVaultResponse saveCredential(String userEmail, CredentialVaultRequest req) {
        validator.validate(req);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        CredentialVault entity = new CredentialVault();
        entity.setUser(user);
        entity.setAppTitle(req.getAppTitle().trim());
        entity.setAppUrl(req.getAppUrl() != null && !req.getAppUrl().isBlank() ? req.getAppUrl().trim() : null);
        entity.setLoginUsername(req.getLoginUsername().trim());
        entity.setEncryptedPassword(cryptoService.encrypt(req.getPassword()));
        entity.setCategory(req.getCategory() != null && !req.getCategory().isBlank() ? req.getCategory().trim().toUpperCase() : "BANKING");
        entity.setNotes(req.getNotes() != null && !req.getNotes().isBlank() ? req.getNotes().trim() : null);

        CredentialVault saved = vaultRepository.save(entity);
        return CredentialVaultResponse.fromEntity(saved, req.getPassword());
    }

    @Transactional(readOnly = true)
    public List<CredentialVaultResponse> getUserCredentials(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return vaultRepository.findByUserIdOrderByAppTitleAsc(user.getId())
                .stream()
                .map(v -> CredentialVaultResponse.fromEntity(v, cryptoService.decrypt(v.getEncryptedPassword())))
                .toList();
    }

    @Transactional
    public void deleteCredential(String userEmail, Long id) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        CredentialVault entity = vaultRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Credential entry not found or access denied."));
        vaultRepository.delete(entity);
    }
}
