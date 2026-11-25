package com.myfinance.security;

import com.myfinance.entity.User;
import com.myfinance.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String clean = identifier == null ? "" : identifier.trim();
        String normalizedPhone = clean.replaceAll("[\\s\\-()]", "");

        User user = users.findByEmailOrUsernameOrPhone(clean)
            .or(() -> users.findByEmailOrUsernameOrPhone(normalizedPhone))
            .orElseThrow(() -> new UsernameNotFoundException("Account not found for provided email, username, or mobile."));

        String[] authorities = user.getRoles().stream()
            .map(role -> role.getName())
            .toArray(String[]::new);

        // Principal username stays canonical email so all downstream services resolve the user seamlessly
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
            .password(user.getPassword())
            .roles(authorities)
            .disabled(!user.isEnabled())
            .build();
    }
}
