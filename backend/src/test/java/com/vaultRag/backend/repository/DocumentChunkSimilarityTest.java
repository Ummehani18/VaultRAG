package com.vaultRag.backend.repository;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.service.EmbeddingService;
import com.vaultRag.backend.service.UserService;
import com.vaultRag.backend.testutil.TestDocumentFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentChunkSimilarityTest {

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private TestDocumentFactory testDocumentFactory;

    @Value("${vaultrag.rag.max-distance}")
    private double maxDistance;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnResultsForRelevantQuestion()
            throws Exception {

        String username =
                "similarity-test-user";

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

        Document document =
                testDocumentFactory
                        .createInsuranceDocument(user);

        assertNotNull(document);

        assertNotNull(document.getUploadedBy());

        assertEquals(
                user.getId(),
                document.getUploadedBy().getId()
        );

        String question =
                "What documents are required for a claim?";

        List<SimilarChunkProjection> results =
                search(
                        question,
                        user
                );

        assertNotNull(results);

        assertFalse(
                results.isEmpty(),
                "Expected relevant chunks to be returned"
        );

        assertTrue(
                results.size() <= 5
        );

        for (SimilarChunkProjection result :
                results) {

            assertNotNull(
                    result.getDistance()
            );

            assertTrue(
                    result.getDistance() <= maxDistance,
                    "Returned result exceeded similarity threshold"
            );
        }
    }

    @Test
    void shouldReturnResultsOnlyFromSelectedDocument()
            throws Exception {

        String username =
                "similarity-document-scope-user";

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

        Document selectedDocument =
                testDocumentFactory
                        .createInsuranceDocument(user);

        Document otherDocument =
                testDocumentFactory
                        .createInsuranceDocument(user);

        assertNotNull(selectedDocument);
        assertNotNull(otherDocument);

        assertNotEquals(
                selectedDocument.getId(),
                otherDocument.getId()
        );

        String question =
                "What documents are required for a claim?";

        List<Double> embedding =
                embeddingService.generateEmbedding(
                        question
                );

        String vector =
                "[" +
                        embedding.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")) +
                        "]";

        List<SimilarChunkProjection> results =
                documentChunkRepository
                        .findSimilarChunksByDocument(
                                vector,
                                maxDistance,
                                5,
                                user.getId(),
                                selectedDocument.getId()
                        );

        assertNotNull(results);

        assertFalse(
                results.isEmpty(),
                "Expected results from selected document"
        );

        assertTrue(
                results.stream()
                        .allMatch(result ->
                                selectedDocument
                                        .getId()
                                        .equals(
                                                result.getDocumentId()
                                        )
                        ),
                "Repository returned a chunk from another document"
        );
    }

    @Test
    void shouldNotReturnSelectedDocumentForAnotherUser()
            throws Exception {

        String ownerUsername =
                "similarity-owner";

        String attackerUsername =
                "similarity-attacker";

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

        User attacker =
                userRepository
                        .findByUsername(attackerUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        attackerUsername,
                                        "test-password-123",
                                        "USER"
                                )
                        );

        Document ownerDocument =
                testDocumentFactory
                        .createInsuranceDocument(owner);

        assertNotNull(ownerDocument);

        String question =
                "What documents are required for a claim?";

        List<Double> embedding =
                embeddingService.generateEmbedding(
                        question
                );

        String vector =
                "[" +
                        embedding.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")) +
                        "]";

        List<SimilarChunkProjection> results =
                documentChunkRepository
                        .findSimilarChunksByDocument(
                                vector,
                                maxDistance,
                                5,
                                attacker.getId(),
                                ownerDocument.getId()
                        );

        assertNotNull(results);

        assertTrue(
                results.isEmpty(),
                "A user must not retrieve another user's document"
        );
    }

    @Test
    void shouldRejectUnrelatedQuestion()
            throws Exception {

        String username =
                "similarity-unrelated-user";

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

        Document document =
                testDocumentFactory
                        .createInsuranceDocument(user);

        assertNotNull(document);

        String question =
                "What is the company's vacation policy?";

        List<SimilarChunkProjection> results =
                search(
                        question,
                        user
                );

        assertNotNull(results);

        assertTrue(
                results.isEmpty(),
                "Expected no results for an unrelated question"
        );
    }

    private List<SimilarChunkProjection> search(
            String question,
            User user) {

        List<Double> embedding =
                embeddingService.generateEmbedding(
                        question
                );

        String vector =
                "[" +
                        embedding.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")) +
                        "]";

        assertNotNull(
                user,
                "Expected test user"
        );

        return documentChunkRepository.findSimilarChunks(
                vector,
                maxDistance,
                5,
                user.getId()
        );
    }
}