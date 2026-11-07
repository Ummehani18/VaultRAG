package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final String DEFAULT_USER_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserService(
            UserRepository userRepository,
            PasswordService passwordService) {

        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    /**
     * Creates a normal user through public registration.
     *
     * Public registration can only create USER accounts.
     */
    public User createUser(
            String username,
            String rawPassword) {

        return createUser(
                username,
                rawPassword,
                DEFAULT_USER_ROLE
        );
    }

    /**
     * Creates a user with an explicitly supplied role.
     *
     * This method is retained for existing internal code and tests.
     * Public registration does NOT expose this method.
     */
    public User createUser(
            String username,
            String rawPassword,
            String role) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters"
            );
        }

        if (rawPassword.length() > 72) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 characters"
            );
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "Role is required"
            );
        }

        String normalizedUsername = username.trim();
        String normalizedRole = role.trim().toUpperCase();

        if (userRepository
                .findByUsername(normalizedUsername)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        User user = new User();

        user.setUsername(normalizedUsername);

        user.setPasswordHash(
                passwordService.hashPassword(rawPassword)
        );

        user.setRole(normalizedRole);

        return userRepository.save(user);
    }

    public User findByUsername(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        return userRepository
                .findByUsername(username.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    public boolean verifyPassword(
            String rawPassword,
            User user) {

        if (rawPassword == null || user == null) {
            return false;
        }

        return passwordService.matches(
                rawPassword,
                user.getPasswordHash()
        );
    }
}