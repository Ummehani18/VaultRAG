package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog log(
            User user,
            String action,
            String resourceType,
            UUID resourceId) {

        AuditLog auditLog = new AuditLog();

        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);

        return auditLogRepository.save(auditLog);
    }
}