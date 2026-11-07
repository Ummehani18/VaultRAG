package com.vaultRag.backend.controller;

import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.AuditLogRepository;
import com.vaultRag.backend.repository.DocumentRepository;
import com.vaultRag.backend.repository.UserRepository;
import com.vaultRag.backend.service.JwtService;
import com.vaultRag.backend.service.UserService;
import com.vaultRag.backend.testutil.TestDocumentFactory;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
class DocumentControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDocumentFactory testDocumentFactory;

    private RestTestClient client;

    private RestTestClient client() {

        if (client == null) {

            client =
                    RestTestClient
                            .bindToServer()
                            .baseUrl(
                                    "http://localhost:" +
                                            port
                            )
                            .build();
        }

        return client;
    }

    @Test
    void ownerCanAccessOwnDocument()
            throws Exception {

        String username =
                "http-document-owner";

        User owner =
                userRepository
                        .findByUsername(username)
                        .orElseGet(() ->
                                userService.createUser(
                                        username,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        Document document =
                getTestDocument(owner);

        String token =
                jwtService.generateToken(
                        username,
                        owner.getRole()
                );

        client()
                .get()
                .uri(
                        "/api/documents/{id}",
                        document.getId()
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(body ->
                        assertTrue(
                                body.contains(
                                        "VaultRAG_Healthcare_Insurance_Test_Dataset.pdf"
                                )
                        )
                );
    }

    @Test
    void anotherUserCannotAccessDocument()
            throws Exception {

        String ownerUsername =
                "http-access-owner";

        String otherUsername =
                "http-access-other";

        User owner =
                userRepository
                        .findByUsername(ownerUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        ownerUsername,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        User otherUser =
                userRepository
                        .findByUsername(otherUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        otherUsername,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        Document document =
                getTestDocument(owner);

        String token =
                jwtService.generateToken(
                        otherUsername,
                        otherUser.getRole()
                );

        client()
                .get()
                .uri(
                        "/api/documents/{id}",
                        document.getId()
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void unauthenticatedUserCannotAccessDocument()
            throws Exception {

        User owner =
                userRepository
                        .findByUsername(
                                "http-unauthenticated-owner"
                        )
                        .orElseGet(() ->
                                userService.createUser(
                                        "http-unauthenticated-owner",
                                        "test-password-123",
                                        "USER"
                                )
                        );

        Document document =
                getTestDocument(owner);

        client()
                .get()
                .uri(
                        "/api/documents/{id}",
                        document.getId()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void nonexistentDocumentReturnsNotFound() {

        String username =
                "http-not-found-user";

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseGet(() ->
                                userService.createUser(
                                        username,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        String token =
                jwtService.generateToken(
                        username,
                        user.getRole()
                );

        client()
                .get()
                .uri(
                        "/api/documents/{id}",
                        UUID.randomUUID()
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void userOnlySeesOwnDocuments()
            throws Exception {

        String userAUsername =
                "list-owner-user";

        String userBUsername =
                "list-other-user";

        User userA =
                userRepository
                        .findByUsername(userAUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        userAUsername,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        User userB =
                userRepository
                        .findByUsername(userBUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        userBUsername,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        Document document =
                getTestDocument(userA);

        String token =
                jwtService.generateToken(
                        userBUsername,
                        userB.getRole()
                );

        client()
                .get()
                .uri("/api/documents")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .value(body ->
                        assertTrue(
                                !body.contains(
                                        "VaultRAG_Healthcare_Insurance_Test_Dataset.pdf"
                                ),
                                "User B must not see User A's document"
                        )
                );
    }

    @Test
    void ownerAccessCreatesDocumentAccessedAuditLog()
            throws Exception {

        String username =
                "http-audit-owner";

        User owner =
                userRepository
                        .findByUsername(username)
                        .orElseGet(() ->
                                userService.createUser(
                                        username,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        Document document =
                getTestDocument(owner);

        String token =
                jwtService.generateToken(
                        username,
                        owner.getRole()
                );

        client()
                .get()
                .uri(
                        "/api/documents/{id}",
                        document.getId()
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isOk();

        boolean auditExists =
                auditLogRepository
                        .findAll()
                        .stream()
                        .anyMatch(log ->
                                "DOCUMENT_ACCESSED"
                                        .equals(log.getAction())
                                        && "DOCUMENT"
                                        .equals(
                                                log.getResourceType()
                                        )
                                        && document.getId()
                                        .equals(
                                                log.getResourceId()
                                        )
                                        && owner.getId()
                                        .equals(
                                                log.getUser().getId()
                                        )
                        );

        assertTrue(
                auditExists,
                "Successful document access must create DOCUMENT_ACCESSED audit log"
        );
    }

    @Test
    void unauthorizedUserDoesNotCreateDocumentAccessedAuditLog()
            throws Exception {

        String ownerUsername =
                "http-audit-access-owner";

        String otherUsername =
                "http-audit-access-other";

        User owner =
                userRepository
                        .findByUsername(ownerUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        ownerUsername,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        User otherUser =
                userRepository
                        .findByUsername(otherUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        otherUsername,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        Document document =
                getTestDocument(owner);

        String token =
                jwtService.generateToken(
                        otherUsername,
                        otherUser.getRole()
                );

        client()
                .get()
                .uri(
                        "/api/documents/{id}",
                        document.getId()
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        boolean unauthorizedAuditExists =
                auditLogRepository
                        .findAll()
                        .stream()
                        .anyMatch(log ->
                                "DOCUMENT_ACCESSED"
                                        .equals(log.getAction())
                                        && "DOCUMENT"
                                        .equals(
                                                log.getResourceType()
                                        )
                                        && document.getId()
                                        .equals(
                                                log.getResourceId()
                                        )
                                        && otherUser.getId()
                                        .equals(
                                                log.getUser().getId()
                                        )
                        );

        assertTrue(
                !unauthorizedAuditExists,
                "Unauthorized document access must not create DOCUMENT_ACCESSED audit log"
        );
    }

    @Test
    void unauthenticatedUserCannotUploadDocument() {

        client()
                .post()
                .uri("/api/documents")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void authenticatedUserCannotUploadNonPdfFile() {

        String username =
                "http-invalid-file-user";

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseGet(() ->
                                userService.createUser(
                                        username,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        String token =
                jwtService.generateToken(
                        username,
                        user.getRole()
                );

        ByteArrayResource resource =
                new ByteArrayResource(
                        "This is not a PDF".getBytes()
                ) {
                    @Override
                    public String getFilename() {
                        return "test.txt";
                    }
                };

        LinkedMultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add(
                "file",
                new HttpEntity<>(resource)
        );

        client()
                .post()
                .uri("/api/documents")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(
                        MediaType.MULTIPART_FORM_DATA
                )
                .body(body)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void authenticatedUserCannotUploadEmptyFile() {

        String username =
                "http-empty-file-user";

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseGet(() ->
                                userService.createUser(
                                        username,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        String token =
                jwtService.generateToken(
                        username,
                        user.getRole()
                );

        ByteArrayResource resource =
                new ByteArrayResource(
                        new byte[0]
                ) {
                    @Override
                    public String getFilename() {
                        return "empty.pdf";
                    }
                };

        LinkedMultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add(
                "file",
                new HttpEntity<>(resource)
        );

        client()
                .post()
                .uri("/api/documents")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(
                        MediaType.MULTIPART_FORM_DATA
                )
                .body(body)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void authenticatedUserCanUploadValidPdf()
            throws Exception {

        String username =
                "http-valid-upload-user";

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseGet(() ->
                                userService.createUser(
                                        username,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        String token =
                jwtService.generateToken(
                        username,
                        user.getRole()
                );

        ClassPathResource pdfResource =
                new ClassPathResource(
                        "test-policy.pdf"
                );

        ByteArrayResource resource =
                new ByteArrayResource(
                        pdfResource
                                .getInputStream()
                                .readAllBytes()
                ) {
                    @Override
                    public String getFilename() {
                        return "test-policy.pdf";
                    }
                };

        LinkedMultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add(
                "file",
                new HttpEntity<>(resource)
        );

        client()
                .post()
                .uri("/api/documents")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(
                        MediaType.MULTIPART_FORM_DATA
                )
                .body(body)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(String.class)
                .value(response -> {

                    assertTrue(
                            response.contains(
                                    "test-policy.pdf"
                            )
                    );

                    assertTrue(
                            response.contains(
                                    "READY"
                            )
                    );
                });
    }

    @Test
    void successfulUploadCreatesDocumentUploadedAuditLog()
            throws Exception {

        String username =
                "http-upload-audit-user";

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseGet(() ->
                                userService.createUser(
                                        username,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        String token =
                jwtService.generateToken(
                        username,
                        user.getRole()
                );

        ClassPathResource pdfResource =
                new ClassPathResource(
                        "test-policy.pdf"
                );

        ByteArrayResource resource =
                new ByteArrayResource(
                        pdfResource
                                .getInputStream()
                                .readAllBytes()
                ) {
                    @Override
                    public String getFilename() {
                        return "audit-test-policy.pdf";
                    }
                };

        LinkedMultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add(
                "file",
                new HttpEntity<>(resource)
        );

        String response =
                client()
                        .post()
                        .uri("/api/documents")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(
                                MediaType.MULTIPART_FORM_DATA
                        )
                        .body(body)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();

        assertTrue(
                response != null
        );

        assertTrue(
                response.contains(
                        "audit-test-policy.pdf"
                )
        );

        boolean auditExists =
                auditLogRepository
                        .findAll()
                        .stream()
                        .anyMatch(log ->
                                "DOCUMENT_UPLOADED"
                                        .equals(log.getAction())
                                        && "DOCUMENT"
                                        .equals(
                                                log.getResourceType()
                                        )
                                        && user.getId()
                                        .equals(
                                                log.getUser().getId()
                                        )
                                        && log.getResourceId() != null
                        );

        assertTrue(
                auditExists,
                "Successful document upload must create DOCUMENT_UPLOADED audit log"
        );
    }

    /**
     * Creates a fresh test document through the real
     * document ingestion pipeline.
     *
     * This avoids relying on a pre-existing document in
     * the test database.
     */
    private Document getTestDocument(
            User owner)
            throws Exception {

        Document document =
                testDocumentFactory
                        .createInsuranceDocument(owner);

        assertTrue(
                document.getUploadedBy() != null,
                "Expected test document to have an owner"
        );

        assertTrue(
                owner.getId().equals(
                        document
                                .getUploadedBy()
                                .getId()
                ),
                "Test document must belong to the requested owner"
        );

        return document;
    }
}