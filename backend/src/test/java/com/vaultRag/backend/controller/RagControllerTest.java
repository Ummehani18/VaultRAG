package com.vaultRag.backend.controller;

import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.AuditLogRepository;
import com.vaultRag.backend.repository.UserRepository;
import com.vaultRag.backend.service.JwtService;
import com.vaultRag.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RagControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    private RestTestClient client;

    private RestTestClient client() {
        if (client == null) {
            client = RestTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }

        return client;
    }

    @Test
    void successfulRagQueryCreatesAuditLog() {

        String username = "rag-audit-test-user";

        User user = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "USER"
                        ));

        String token = jwtService.generateToken(
                username,
                user.getRole()
        );

        String question =
                "What are the pre-authorization rules?";

        client()
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/api/rag")
                                .queryParam("question", question)
                                .queryParam("limit", 3)
                                .build()
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk();

        List<AuditLog> auditLogs =
                auditLogRepository.findAll()
                        .stream()
                        .filter(log ->
                                "RAG_QUERY_EXECUTED".equals(log.getAction())
                                        && "RAG".equals(log.getResourceType())
                                        && log.getResourceId() == null
                                        && user.getId().equals(
                                        log.getUser().getId()
                                )
                        )
                        .toList();

        assertFalse(
                auditLogs.isEmpty(),
                "Successful RAG query must create RAG_QUERY_EXECUTED audit log"
        );

        AuditLog auditLog = auditLogs.get(auditLogs.size() - 1);

        assertNotNull(auditLog.getId());
        assertEquals(user.getId(), auditLog.getUser().getId());
        assertEquals("RAG_QUERY_EXECUTED", auditLog.getAction());
        assertEquals("RAG", auditLog.getResourceType());
        assertNull(auditLog.getResourceId());
        assertNotNull(auditLog.getCreatedAt());
    }
}