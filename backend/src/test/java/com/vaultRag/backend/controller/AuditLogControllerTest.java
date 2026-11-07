package com.vaultRag.backend.controller;

import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.UserRepository;
import com.vaultRag.backend.service.AuditLogService;
import com.vaultRag.backend.service.JwtService;
import com.vaultRag.backend.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditLogControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

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
    void adminCanAccessAuditLogs() {

        String username = "audit-admin-test";

        User admin = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "ADMIN"
                        )
                );

        String token = jwtService.generateToken(
                username,
                admin.getRole()
        );

        client()
                .get()
                .uri("/api/audit-logs")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void regularUserCanAccessOwnAuditLogs() {

        String username = "audit-personal-user";

        User user = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "USER"
                        )
                );

        auditLogService.log(
                user,
                "PERSONAL_TEST_ACTION",
                "TEST",
                UUID.randomUUID()
        );

        String token = jwtService.generateToken(
                username,
                user.getRole()
        );

        client()
                .get()
                .uri("/api/audit-logs")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content")
                .isArray()
                .jsonPath("$.content[0].action")
                .isEqualTo("PERSONAL_TEST_ACTION")
                .jsonPath("$.content[0].username")
                .isEqualTo(username);
    }

    @Test
    void regularUserCanOnlySeeOwnAuditLogs() {

        String user1Username = "audit-isolation-user-1";
        String user2Username = "audit-isolation-user-2";

        User user1 = userRepository.findByUsername(user1Username)
                .orElseGet(() ->
                        userService.createUser(
                                user1Username,
                                "test-password-123",
                                "USER"
                        )
                );

        User user2 = userRepository.findByUsername(user2Username)
                .orElseGet(() ->
                        userService.createUser(
                                user2Username,
                                "test-password-123",
                                "USER"
                        )
                );

        auditLogService.log(
                user1,
                "USER_ONE_ACTION",
                "TEST",
                UUID.randomUUID()
        );

        auditLogService.log(
                user2,
                "USER_TWO_ACTION",
                "TEST",
                UUID.randomUUID()
        );

        String token = jwtService.generateToken(
                user1Username,
                user1.getRole()
        );

        client()
                .get()
                .uri("/api/audit-logs")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content")
                .value(content -> {

                    @SuppressWarnings("unchecked")
                    var logs = (java.util.List<java.util.Map<String, Object>>) content;

                    assertTrue(
                            logs.stream().allMatch(
                                    log -> user1Username.equals(
                                            log.get("username")
                                    )
                            )
                    );

                    assertTrue(
                            logs.stream().noneMatch(
                                    log -> user2Username.equals(
                                            log.get("username")
                                    )
                            )
                    );
                });
    }

    @Test
    void unauthenticatedUserCannotAccessAuditLogs() {

        client()
                .get()
                .uri("/api/audit-logs")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void adminCanPaginateAuditLogs() {

        String username = "pagination-admin";

        User admin = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "ADMIN"
                        )
                );

        /*
         * Create enough audit records to verify
         * that pagination actually works.
         */
        for (int i = 0; i < 3; i++) {
            auditLogService.log(
                    admin,
                    "TEST_ACTION_" + i,
                    "TEST",
                    UUID.randomUUID()
            );
        }

        String token = jwtService.generateToken(
                admin.getUsername(),
                admin.getRole()
        );

        client()
                .get()
                .uri("/api/audit-logs?page=0&size=2")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.page")
                .isEqualTo(0)
                .jsonPath("$.size")
                .isEqualTo(2)
                .jsonPath("$.totalElements")
                .value(total ->
                        assertTrue((Integer) total >= 3)
                )
                .jsonPath("$.totalPages")
                .value(total ->
                        assertTrue((Integer) total >= 2)
                )
                .jsonPath("$.first")
                .isEqualTo(true)
                .jsonPath("$.last")
                .isEqualTo(false)
                .jsonPath("$.content.length()")
                .isEqualTo(2);
    }

    @Test
    void adminCannotRequestNegativePage() {

        String username = "audit-validation-admin";

        User admin = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "ADMIN"
                        )
                );

        String token = jwtService.generateToken(
                username,
                admin.getRole()
        );

        client()
                .get()
                .uri("/api/audit-logs?page=-1&size=20")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.error")
                .isEqualTo(
                        "Page must be greater than or equal to 0"
                );
    }

    @Test
    void adminCannotRequestMoreThanMaximumPageSize() {

        String username = "audit-size-validation-admin";

        User admin = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "ADMIN"
                        )
                );

        String token = jwtService.generateToken(
                username,
                admin.getRole()
        );

        client()
                .get()
                .uri("/api/audit-logs?page=0&size=101")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.error")
                .isEqualTo(
                        "Size must be between 1 and 100"
                );
    }
}