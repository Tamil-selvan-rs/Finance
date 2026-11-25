package com.myfinance.validator;

import com.myfinance.dto.RegistrationRequest;
import com.myfinance.exception.BusinessValidationException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class RegistrationValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,60}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{8,15}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,72}$");

    public void validate(RegistrationRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (req.getFullName() == null || req.getFullName().trim().length() < 2) {
            errors.put("fullName", "Full name must be at least 2 characters.");
        }
        if (req.getUsername() != null && !req.getUsername().isBlank()
                && !USERNAME_PATTERN.matcher(req.getUsername().trim()).matches()) {
            errors.put("username", "Username must be 3-60 characters (letters, digits, dot, underscore, hyphen).");
        }
        if (req.getEmail() == null || !EMAIL_PATTERN.matcher(req.getEmail().trim()).matches()) {
            errors.put("email", "Provide a valid email address.");
        }
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            String cleanPhone = req.getPhone().replaceAll("[\\s\\-()]", "");
            if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                errors.put("phone", "Enter a valid mobile number (8-15 digits).");
            }
        }
        if (req.getPassword() == null || !STRONG_PASSWORD.matcher(req.getPassword()).matches()) {
            errors.put("password", "Password must be 8-72 chars with uppercase, lowercase, digit, and special character.");
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Registration details failed validation.", errors);
        }
    }
}
