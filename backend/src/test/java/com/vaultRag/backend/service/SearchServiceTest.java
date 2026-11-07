package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentRepository;
import com.vaultRag.backend.repository.SimilarChunkProjection;
import com.vaultRag.backend.repository.UserRepository;
import com.vaultRag.backend.testutil.TestDocumentFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Autowired
    private TestDocumentFactory testDocumentFactory;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSearchRelevantChunks()
            throws Exception {

        String username =
                "search-test-user";

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

        Document testDocument =
                testDocumentFactory
                        .createInsuranceDocument(user);

        assertNotNull(testDocument);

        assertNotNull(testDocument.getUploadedBy());

        assertEquals(
                user.getId(),
                testDocument.getUploadedBy().getId()
        );

        authenticate(user);

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

        assertTrue(
                results.size() <= 3
        );

        System.out.println(
                "\n========== SEARCH RESULTS =========="
        );

        printResults(results);
    }

    @Test
    void shouldSearchOnlySelectedDocument()
            throws Exception {

        String username =
                "document-scope-test-user";

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

        Document insuranceDocument =
                testDocumentFactory
                        .createInsuranceDocument(user);

        Document secondDocument =
                testDocumentFactory
                        .createInsuranceDocument(user);

        assertNotNull(insuranceDocument);
        assertNotNull(secondDocument);

        assertNotEquals(
                insuranceDocument.getId(),
                secondDocument.getId()
        );

        authenticate(user);

        String question =
                "What documents are required for a claim?";

        List<SimilarChunkProjection> results =
                searchService.search(
                        question,
                        5,
                        insuranceDocument.getId()
                );

        assertNotNull(results);

        assertFalse(
                results.isEmpty(),
                "Expected results from selected document"
        );

        /*
         * Every retrieved result must belong to the
         * selected document.
         */
        assertTrue(
                results.stream()
                        .allMatch(result ->
                                insuranceDocument
                                        .getId()
                                        .equals(
                                                result.getDocumentId()
                                        )
                        ),
                "Search returned chunks from a document other than the selected document"
        );

        System.out.println(
                "\n========== DOCUMENT-SCOPED SEARCH =========="
        );

        System.out.println(
                "Selected document: " +
                        insuranceDocument.getId()
        );

        printResults(results);
    }

    @Test
    void shouldNotReturnDocumentsOwnedByAnotherUser()
            throws Exception {

        String ownerUsername =
                "security-owner";

        String attackerUsername =
                "security-other-user";

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

        assertNotNull(ownerDocument.getUploadedBy());

        assertEquals(
                owner.getId(),
                ownerDocument.getUploadedBy().getId()
        );

        authenticate(attacker);

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

    @Test
    void shouldRejectSelectedDocumentOwnedByAnotherUser()
            throws Exception {

        String ownerUsername =
                "selected-document-owner";

        String attackerUsername =
                "selected-document-attacker";

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

        authenticate(attacker);

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () ->
                        searchService.search(
                                "What documents are required for a claim?",
                                5,
                                ownerDocument.getId()
                        )
        );
    }

    private void authenticate(User user) {

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" +
                                                        user.getRole()
                                        )
                                )
                        )
                );
    }

    private void printResults(
            List<SimilarChunkProjection> results) {

        for (SimilarChunkProjection result :
                results) {

            System.out.println(
                    "\nDistance: " +
                            result.getDistance()
            );

            System.out.println(
                    "Document: " +
                            result.getDocumentName()
            );

            System.out.println(
                    "Document ID: " +
                            result.getDocumentId()
            );

            System.out.println(
                    "Chunk: " +
                            result.getChunkIndex()
            );

            System.out.println(
                    "Page: " +
                            result.getPageNumber()
            );

            System.out.println(
                    "Content: " +
                            result.getContent()
            );

            System.out.println(
                    "----------------------------------------"
            );
        }
    }
}