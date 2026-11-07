package com.vaultRag.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String role,
        LocalDateTime createdAt
) {
}