package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.AuditLogRepository;
import com.vaultRag.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.AuditLogRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditLogServiceTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    void shouldCreateAuditLog() {

        String username = "audit-test-user";

        User user = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "USER"
                        ));

        UUID documentId = UUID.randomUUID();

        AuditLog auditLog = auditLogService.log(
                user,
                "DOCUMENT_UPLOADED",
                "DOCUMENT",
                documentId
        );

        assertNotNull(auditLog);
        assertNotNull(auditLog.getId());

        assertEquals(
                user.getId(),
                auditLog.getUser().getId()
        );

        assertEquals(
                "DOCUMENT_UPLOADED",
                auditLog.getAction()
        );

        assertEquals(
                "DOCUMENT",
                auditLog.getResourceType()
        );

        assertEquals(
                documentId,
                auditLog.getResourceId()
        );

        assertNotNull(auditLog.getCreatedAt());

        assertTrue(
                auditLogRepository.existsById(auditLog.getId())
        );
    }
}