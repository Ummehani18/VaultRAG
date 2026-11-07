package com.vaultRag.backend.service;

import com.vaultRag.backend.dto.RagResponse;
import com.vaultRag.backend.dto.RagSource;
import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.UserRepository;
import com.vaultRag.backend.testutil.TestDocumentFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    private TestDocumentFactory testDocumentFactory;

    @Autowired
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAnswerQuestionUsingRetrievedContext()
            throws Exception {

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
    }

    @Test
    void shouldAnswerQuestionUsingSelectedDocument()
            throws Exception {

        User user =
                authenticateAsTestUser();

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

        RagResponse response =
                ragService.answer(
                        question,
                        3,
                        selectedDocument.getId()
                );

        assertNotNull(response);

        assertNotNull(response.sources());

        assertFalse(
                response.sources().isEmpty(),
                "Expected sources from selected document"
        );

        assertTrue(
                response.sources()
                        .stream()
                        .allMatch(source ->
                                selectedDocument
                                        .getId()
                                        .equals(
                                                source.documentId()
                                        )
                        ),
                "RAG returned a source from a document other than the selected document"
        );
    }

    @Test
    void shouldReturnSafeResponseWhenNoRelevantContextExists()
            throws Exception {

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
    }

    @Test
    void shouldReturnPageNumberForRetrievedSource()
            throws Exception {

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

        assertFalse(
                response.sources().isEmpty(),
                "Expected at least one relevant source"
        );

        RagSource pageSevenSource =
                response.sources()
                        .stream()
                        .filter(source ->
                                "VaultRAG_Healthcare_Insurance_Test_Dataset.pdf"
                                        .equals(
                                                source.documentName()
                                        )
                        )
                        .filter(source ->
                                Integer.valueOf(7)
                                        .equals(
                                                source.pageNumber()
                                        )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Expected a source from page 7"
                                )
                        );

        assertEquals(
                "VaultRAG_Healthcare_Insurance_Test_Dataset.pdf",
                pageSevenSource.documentName()
        );

        assertEquals(
                7,
                pageSevenSource.pageNumber()
        );

        assertNotNull(
                pageSevenSource.content()
        );

        assertFalse(
                pageSevenSource.content().isBlank()
        );
    }

    @Test
    void shouldNotAnswerUsingAnotherUsersDocument()
            throws Exception {

        String ownerUsername =
                "rag-security-owner";

        String otherUserUsername =
                "rag-security-other-user";

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
                        .findByUsername(otherUserUsername)
                        .orElseGet(() ->
                                userService.createUser(
                                        otherUserUsername,
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

        authenticate(otherUser);

        RagResponse response =
                ragService.answer(
                        "What documents are required for a claim?",
                        3
                );

        assertNotNull(response);

        assertNotNull(response.sources());

        assertTrue(
                response.sources().isEmpty(),
                "RAG must not return another user's sources"
        );

        assertEquals(
                "I could not find relevant information in the provided documents.",
                response.answer()
        );
    }

    @Test
    void shouldRejectSelectedDocumentOwnedByAnotherUser()
            throws Exception {

        User owner =
                userRepository
                        .findByUsername(
                                "rag-selected-owner"
                        )
                        .orElseGet(() ->
                                userService.createUser(
                                        "rag-selected-owner",
                                        "test-password-123",
                                        "USER"
                                )
                        );

        User attacker =
                userRepository
                        .findByUsername(
                                "rag-selected-attacker"
                        )
                        .orElseGet(() ->
                                userService.createUser(
                                        "rag-selected-attacker",
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
                AccessDeniedException.class,
                () ->
                        ragService.answer(
                                "What documents are required for a claim?",
                                3,
                                ownerDocument.getId()
                        )
        );
    }

    private User authenticateAsTestUser()
            throws Exception {

        User user =
                userRepository
                        .findByUsername("rag-test-user")
                        .orElseGet(() ->
                                userService.createUser(
                                        "rag-test-user",
                                        "password",
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

        authenticate(user);

        return user;
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
}