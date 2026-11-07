package com.vaultRag.backend.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long totalAdmins,
        long totalDocuments,
        long totalAuditLogs
) {
}