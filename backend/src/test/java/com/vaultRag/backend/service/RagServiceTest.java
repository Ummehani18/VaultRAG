package com.vaultRag.backend.service;

import com.vaultRag.backend.dto.RagResponse;
import com.vaultRag.backend.dto.RagSource;
import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentRepository;
import com.vaultRag.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RagServiceTest {

    @Autowired
    private RagService ragService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAnswerQuestionUsingRetrievedContext() {

        authenticateAsTestUser();

        String question =
                "What documents are required for a claim?";

        RagResponse response =
                ragService.answer(
                        question,
                        3
                );

        assertNotNull(response);

        assertNotNull(response.answer());
        assertFalse(response.answer().isBlank());

        assertNotNull(response.sources());
        assertFalse(response.sources().isEmpty());

        System.out.println(
                "\n========== RAG ANSWER =========="
        );

        System.out.println(response.answer());

        System.out.println(
                "\n========== SOURCES =========="
        );

        for (RagSource source : response.sources()) {

            System.out.println(
                    "\nDocument: " +
                            source.documentName()
            );

            System.out.println(
                    "Chunk: " +
                            source.chunkIndex()
            );

            System.out.println(
                    "Page: " +
                            source.pageNumber()
            );

            System.out.println(
                    "Content: " +
                            source.content()
            );
        }
    }

    @Test
    void shouldReturnSafeResponseWhenNoRelevantContextExists() {

        authenticateAsTestUser();

        String question =
                "What is the company's vacation policy?";

        RagResponse response =
                ragService.answer(
                        question,
                        3
                );

        assertNotNull(response);

        assertEquals(
                "I could not find relevant information in the provided documents.",
                response.answer()
        );

        assertNotNull(response.sources());

        assertTrue(
                response.sources().isEmpty(),
                "Expected no sources for an unrelated question"
        );

        System.out.println(
                "\n========== NO RELEVANT CONTEXT =========="
        );

        System.out.println(
                "Question: " + question
        );

        System.out.println(
                "Answer: " + response.answer()
        );

        System.out.println(
                "Sources: " + response.sources().size()
        );
    }

    @Test
    void shouldReturnPageNumberForRetrievedSource() {

        authenticateAsTestUser();

        String question =
                "What are the reimbursement rules?";

        RagResponse response =
                ragService.answer(
                        question,
                        3
                );

        assertNotNull(response);
        assertNotNull(response.sources());
        assertFalse(response.sources().isEmpty());

        RagSource pageThreeSource =
                response.sources()
                        .stream()
                        .filter(source ->
                                "VaultRAG_3_Page_Insurance_Test.pdf"
                                        .equals(source.documentName())
                        )
                        .filter(source ->
                                Integer.valueOf(3)
                                        .equals(source.pageNumber())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Expected a source from page 3"
                                )
                        );

        assertEquals(
                "VaultRAG_3_Page_Insurance_Test.pdf",
                pageThreeSource.documentName()
        );

        assertEquals(
                3,
                pageThreeSource.pageNumber()
        );

        assertTrue(
                pageThreeSource.content()
                        .contains("Reimbursement Rules")
        );

        System.out.println(
                "\n========== PAGE CITATION REGRESSION =========="
        );

        System.out.println(
                "Document: " +
                        pageThreeSource.documentName()
        );

        System.out.println(
                "Page: " +
                        pageThreeSource.pageNumber()
        );

        System.out.println(
                "Chunk: " +
                        pageThreeSource.chunkIndex()
        );
    }

    @Test
    void shouldNotAnswerUsingAnotherUsersDocument() {

        String ownerUsername = "rag-security-owner";
        String otherUserUsername = "rag-security-other-user";

        User owner = userRepository
                .findByUsername(ownerUsername)
                .orElseGet(() ->
                        userService.createUser(
                                ownerUsername,
                                "test-password-123",
                                "USER"
                        )
                );

        User otherUser = userRepository
                .findByUsername(otherUserUsername)
                .orElseGet(() ->
                        userService.createUser(
                                otherUserUsername,
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
                        otherUserUsername,
                        null,
                        List.of()
                )
        );

        try {

            RagResponse response =
                    ragService.answer(
                            "What documents are required for a claim?",
                            3
                    );

            assertNotNull(response);

            assertTrue(
                    response.sources().isEmpty(),
                    "RAG must not return another user's sources"
            );

            assertEquals(
                    "I could not find relevant information in the provided documents.",
                    response.answer()
            );

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateAsTestUser() {

        String username = "rag-test-user";

        User user = userRepository
                .findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "USER"
                        )
                );

        Document document = documentRepository.findAll()
                .stream()
                .filter(existingDocument ->
                        "VaultRAG_3_Page_Insurance_Test.pdf"
                                .equals(existingDocument.getName())
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Expected VaultRAG_3_Page_Insurance_Test.pdf"
                        )
                );

        document.setUploadedBy(user);
        documentRepository.saveAndFlush(document);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of()
                )
        );
    }
}