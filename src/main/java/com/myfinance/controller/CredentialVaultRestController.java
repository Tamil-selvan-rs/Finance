package com.myfinance.controller;

import com.myfinance.dto.CredentialVaultRequest;
import com.myfinance.dto.CredentialVaultResponse;
import com.myfinance.service.CredentialVaultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vault")
public class CredentialVaultRestController {

    private final CredentialVaultService vaultService;

    public CredentialVaultRestController(CredentialVaultService vaultService) {
        this.vaultService = vaultService;
    }

    @GetMapping
    public ResponseEntity<List<CredentialVaultResponse>> list(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(vaultService.getUserCredentials(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CredentialVaultResponse> create(@AuthenticationPrincipal UserDetails user,
                                                          @RequestBody CredentialVaultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaultService.saveCredential(user.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        vaultService.deleteCredential(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
