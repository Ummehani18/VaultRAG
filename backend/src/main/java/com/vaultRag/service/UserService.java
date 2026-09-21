package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserService(
            UserRepository userRepository,
            PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    public User createUser(
            String username,
            String rawPassword,
            String role) {

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(
                passwordService.hashPassword(rawPassword)
        );
        user.setRole(role);

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    public boolean verifyPassword(
            String rawPassword,
            User user) {

        return passwordService.matches(
                rawPassword,
                user.getPasswordHash()
        );
    }
}