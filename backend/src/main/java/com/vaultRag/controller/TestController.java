package com.vaultRag.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public Map<String, String> protectedEndpoint(
            Authentication authentication) {

        return Map.of(
                "message", "JWT authentication successful",
                "username", authentication.getName()
        );
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public Map<String, String> userEndpoint(
            Authentication authentication) {

        return Map.of(
                "message", "USER access granted",
                "username", authentication.getName()
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminEndpoint(
            Authentication authentication) {

        return Map.of(
                "message", "ADMIN access granted",
                "username", authentication.getName()
        );
    }
}