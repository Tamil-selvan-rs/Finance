package com.myfinance.validator;

import com.myfinance.dto.CredentialVaultRequest;
import com.myfinance.exception.BusinessValidationException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CredentialVaultValidator {

    public void validate(CredentialVaultRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (req == null) {
            throw new BusinessValidationException("Credential payload cannot be null.", errors);
        }
        if (req.getAppTitle() == null || req.getAppTitle().trim().length() < 2 || req.getAppTitle().trim().length() > 120) {
            errors.put("appTitle", "Application title must be between 2 and 120 characters.");
        }
        if (req.getLoginUsername() == null || req.getLoginUsername().trim().isEmpty() || req.getLoginUsername().trim().length() > 150) {
            errors.put("loginUsername", "Username or email is required (max 150 characters).");
        }
        if (req.getPassword() == null || req.getPassword().isEmpty() || req.getPassword().length() > 200) {
            errors.put("password", "Password is required (max 200 characters).");
        }
        if (req.getAppUrl() != null && !req.getAppUrl().isBlank() && req.getAppUrl().length() > 255) {
            errors.put("appUrl", "Application URL cannot exceed 255 characters.");
        }
        if (req.getNotes() != null && req.getNotes().length() > 500) {
            errors.put("notes", "Notes cannot exceed 500 characters.");
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Credential vault validation failed.", errors);
        }
    }
}
