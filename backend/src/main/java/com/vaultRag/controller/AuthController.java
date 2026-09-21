package com.vaultRag.backend.controller;

import com.vaultRag.backend.dto.LoginRequest;
import com.vaultRag.backend.dto.RegisterRequest;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vaultRag.backend.service.JwtService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    private final JwtService jwtService;

    public AuthController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        try {
            User user = userService.createUser(
                    request.getUsername(),
                    request.getPassword(),
                    "USER"
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "role", user.getRole()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", e.getMessage()
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            User user = userService.findByUsername(
                    request.getUsername()
            );

            boolean passwordMatches =
                    userService.verifyPassword(
                            request.getPassword(),
                            user
                    );

            if (!passwordMatches) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Invalid username or password"
                        ));
            }

            String token = jwtService.generateToken(
                    user.getUsername(),
                    user.getRole()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "token", token,
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "role", user.getRole()
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Invalid username or password"
                    ));
        }
    }
}