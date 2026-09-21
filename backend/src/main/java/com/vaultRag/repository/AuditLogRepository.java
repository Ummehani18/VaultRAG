package com.vaultRag.backend.repository;

import com.vaultRag.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}