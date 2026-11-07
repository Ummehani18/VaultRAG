package com.vaultRag.backend.controller;

import com.vaultRag.backend.dto.AuditLogResponse;
import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserAuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AdminUserAuditLogController(
            AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/{userId}/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getUserAuditLogs(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Page<AuditLog> auditLogPage =
                auditLogRepository.findByUserId(
                        userId,
                        pageable
                );

        List<AuditLogResponse> content =
                auditLogPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return Map.of(
                "content", content,
                "page", auditLogPage.getNumber(),
                "size", auditLogPage.getSize(),
                "totalElements",
                auditLogPage.getTotalElements(),
                "totalPages",
                auditLogPage.getTotalPages(),
                "first",
                auditLogPage.isFirst(),
                "last",
                auditLogPage.isLast()
        );
    }

    private AuditLogResponse toResponse(
            AuditLog auditLog) {

        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUser() != null
                        ? auditLog.getUser().getId()
                        : null,
                auditLog.getUser() != null
                        ? auditLog.getUser().getUsername()
                        : null,
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getCreatedAt()
        );
    }
}