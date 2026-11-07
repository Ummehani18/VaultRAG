package com.vaultRag.backend.controller;

import com.vaultRag.backend.dto.AuditLogResponse;
import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.AuditLogRepository;
import com.vaultRag.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    public AuditLogController(
            AuditLogRepository auditLogRepository,
            UserService userService) {

        this.auditLogRepository = auditLogRepository;
        this.userService = userService;
    }

    @GetMapping
    public Map<String, Object> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

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

        User currentUser =
                userService.findByUsername(authentication.getName());

        Page<AuditLog> auditLogPage;

        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {

            // Admin can see audit logs from every user.
            auditLogPage =
                    auditLogRepository.findAll(pageable);

        } else {

            // Regular users can only see their own audit logs.
            auditLogPage =
                    auditLogRepository.findByUserId(
                            currentUser.getId(),
                            pageable
                    );
        }

        List<AuditLogResponse> content =
                auditLogPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return Map.of(
                "content", content,
                "page", auditLogPage.getNumber(),
                "size", auditLogPage.getSize(),
                "totalElements", auditLogPage.getTotalElements(),
                "totalPages", auditLogPage.getTotalPages(),
                "first", auditLogPage.isFirst(),
                "last", auditLogPage.isLast()
        );
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {

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