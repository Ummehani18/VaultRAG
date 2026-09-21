package com.vaultRag.backend.service;

import com.vaultRag.backend.repository.SimilarChunkProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentRepository;
import com.vaultRag.backend.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void shouldSearchRelevantChunks() {

        String username = "search-test-user";

        User user = userRepository
                .findByUsername(username)
                .orElseGet(() -> userService.createUser(
                        username,
                        "test-password-123",
                        "USER"
                ));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of()
                )
        );

        Document testDocument = documentRepository.findAll()
                .stream()
                .filter(document ->
                        "VaultRAG_3_Page_Insurance_Test.pdf"
                                .equals(document.getName())
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Expected VaultRAG_3_Page_Insurance_Test.pdf"
                        )
                );

        testDocument.setUploadedBy(user);
        documentRepository.saveAndFlush(testDocument);

        String question =
                "What documents are required for a claim?";

        List<SimilarChunkProjection> results =
                searchService.search(
                        question,
                        3
                );

        assertNotNull(results);

        assertFalse(
                results.isEmpty(),
                "Expected at least one search result"
        );

        assertTrue(results.size() <= 3);

        System.out.println(
                "\n========== SEARCH RESULTS =========="
        );

        for (SimilarChunkProjection result : results) {

            System.out.println(
                    "\nDistance: " +
                            result.getDistance()
            );

            System.out.println(
                    "Document: " +
                            result.getDocumentName()
            );

            System.out.println(
                    "Chunk: " +
                            result.getChunkIndex()
            );

            System.out.println(
                    "Content: " +
                            result.getContent()
            );

            System.out.println(
                    "----------------------------------------"
            );
        }

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotReturnDocumentsOwnedByAnotherUser() {

        String ownerUsername = "security-owner";
        String attackerUsername = "security-other-user";

        User owner = userRepository
                .findByUsername(ownerUsername)
                .orElseGet(() ->
                        userService.createUser(
                                ownerUsername,
                                "test-password-123",
                                "USER"
                        )
                );

        User attacker = userRepository
                .findByUsername(attackerUsername)
                .orElseGet(() ->
                        userService.createUser(
                                attackerUsername,
                                "test-password-123",
                                "USER"
                        )
                );

        Document ownerDocument = documentRepository.findAll()
                .stream()
                .filter(document ->
                        "VaultRAG_3_Page_Insurance_Test.pdf"
                                .equals(document.getName())
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Expected test insurance document"
                        )
                );

        ownerDocument.setUploadedBy(owner);
        documentRepository.saveAndFlush(ownerDocument);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        attackerUsername,
                        null,
                        List.of()
                )
        );

        try {

            List<SimilarChunkProjection> results =
                    searchService.search(
                            "What documents are required for a claim?",
                            5
                    );

            assertNotNull(results);

            assertTrue(
                    results.isEmpty(),
                    "User must not retrieve another user's documents"
            );

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}