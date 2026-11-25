package com.myfinance.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultAdminInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // 1. Create default roles if they do not exist
        jdbcTemplate.update(
                "INSERT IGNORE INTO roles (name) VALUES ('USER'), ('ADMIN')"
        );

        // 2. Default admin details
        String fullName = "System Administrator";
        String email = "admin@myfinance.com";
        String rawPassword = "Admin@123";
        String phone = "9999999999";

        // 3. Check whether admin user already exists
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );

        // 4. Create user only if it does not exist
        if (userCount != null && userCount == 0) {

            String encryptedPassword =
                    passwordEncoder.encode(rawPassword);

            jdbcTemplate.update(
                    "INSERT INTO users " +
                            "(full_name, email, password, phone, enabled) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    fullName,
                    email,
                    encryptedPassword,
                    phone,
                    true
            );

            System.out.println("Default admin user created.");
        }

        // 5. Assign ADMIN role if not already assigned
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) " +
                        "SELECT u.id, r.id " +
                        "FROM users u " +
                        "JOIN roles r ON r.name = 'ADMIN' " +
                        "WHERE u.email = ? " +
                        "AND NOT EXISTS (" +
                        "SELECT 1 FROM user_roles ur " +
                        "WHERE ur.user_id = u.id AND ur.role_id = r.id" +
                        ")",
                email
        );

        System.out.println("Default admin initialization completed.");
    }
}
