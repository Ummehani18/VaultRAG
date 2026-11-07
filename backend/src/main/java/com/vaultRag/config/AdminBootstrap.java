package com.vaultRag.backend.config;

import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.UserRepository;
import com.vaultRag.backend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${vaultrag.admin.bootstrap-enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${vaultrag.admin.username:}")
    private String adminUsername;

    @Value("${vaultrag.admin.password:}")
    private String adminPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            UserService userService) {

        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {

        if (!bootstrapEnabled) {
            return;
        }

        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled, but VAULTRAG_ADMIN_USERNAME is not configured"
            );
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled, but VAULTRAG_ADMIN_PASSWORD is not configured"
            );
        }

        if (userRepository.existsByRoleIgnoreCase("ADMIN")) {
            System.out.println(
                    "VaultRAG admin bootstrap skipped: an ADMIN already exists."
            );
            return;
        }

        User admin = userService.createUser(
                adminUsername,
                adminPassword,
                "ADMIN"
        );

        System.out.println(
                "VaultRAG initial ADMIN created: "
                        + admin.getUsername()
        );
    }
}