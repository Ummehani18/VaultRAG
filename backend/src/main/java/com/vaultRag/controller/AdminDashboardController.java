package com.vaultRag.backend.controller;

import com.vaultRag.backend.dto.AdminDashboardResponse;
import com.vaultRag.backend.repository.AuditLogRepository;
import com.vaultRag.backend.repository.DocumentRepository;
import com.vaultRag.backend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardController(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            AuditLogRepository auditLogRepository) {

        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardResponse getDashboard() {

        long totalUsers =
                userRepository.count();

        long totalAdmins =
                userRepository.countByRoleIgnoreCase("ADMIN");

        long totalDocuments =
                documentRepository.count();

        long totalAuditLogs =
                auditLogRepository.count();

        return new AdminDashboardResponse(
                totalUsers,
                totalAdmins,
                totalDocuments,
                totalAuditLogs
        );
    }
}