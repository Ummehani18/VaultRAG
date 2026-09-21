package com.vaultRag.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String username,
        String action,
        String resourceType,
        UUID resourceId,
        LocalDateTime createdAt
) {
}