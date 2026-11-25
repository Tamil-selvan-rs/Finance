package com.myfinance.service;

import com.myfinance.dto.RegistrationRequest;
import com.myfinance.entity.Role;
import com.myfinance.entity.User;
import com.myfinance.exception.BusinessValidationException;
import com.myfinance.exception.ErrorCode;
import com.myfinance.repository.RoleRepository;
import com.myfinance.repository.UserRepository;
import com.myfinance.validator.RegistrationValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationValidator registrationValidator;

    public UserService(UserRepository users,
                       RoleRepository roles,
                       PasswordEncoder passwordEncoder,
                       RegistrationValidator registrationValidator) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.registrationValidator = registrationValidator;
    }

    @Transactional
    public void register(RegistrationRequest request) {
        registrationValidator.validate(request);

        String email = request.getEmail().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new BusinessValidationException(ErrorCode.DUPLICATE_RESOURCE, "An account with this email already exists.");
        }

        String username = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername().trim().toLowerCase()
                : null;
        if (username != null && users.existsByUsername(username)) {
            throw new BusinessValidationException(ErrorCode.DUPLICATE_RESOURCE, "This username is already taken.");
        }

        String cleanPhone = request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone().replaceAll("[\\s\\-()]", "")
                : null;
        if (cleanPhone != null && users.existsByPhone(cleanPhone)) {
            throw new BusinessValidationException(ErrorCode.DUPLICATE_RESOURCE, "This mobile number is already registered.");
        }

        Role userRole = roles.findByName("USER")
            .orElseThrow(() -> new IllegalStateException("USER role missing. Run database migrations first."));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(cleanPhone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.getRoles().add(userRole);
        users.save(user);
    }
}
