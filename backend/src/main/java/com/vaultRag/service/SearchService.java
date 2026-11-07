package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentChunkRepository;
import com.vaultRag.backend.repository.DocumentRepository;
import com.vaultRag.backend.repository.SimilarChunkProjection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger log =
            LoggerFactory.getLogger(SearchService.class);

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final UserService userService;

    @Value("${vaultrag.rag.max-distance}")
    private double maxDistance;

    public SearchService(
            EmbeddingService embeddingService,
            DocumentChunkRepository documentChunkRepository,
            DocumentRepository documentRepository,
            UserService userService) {

        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.userService = userService;
    }

    /**
     * Search across all documents owned by the authenticated user.
     */
    public List<SimilarChunkProjection> search(
            String question,
            int limit) {

        User user = getAuthenticatedUser();

        return search(question, limit, user);
    }

    /**
     * Search within a selected document, or all owned documents
     * when documentId is null.
     */
    public List<SimilarChunkProjection> search(
            String question,
            int limit,
            UUID documentId) {

        User user = getAuthenticatedUser();

        return search(question, limit, user, documentId);
    }

    /**
     * Search across all documents belonging to the supplied user.
     */
    public List<SimilarChunkProjection> search(
            String question,
            int limit,
            User user) {

        return search(question, limit, user, null);
    }

    /**
     * Main vector-search implementation.
     */
    public List<SimilarChunkProjection> search(
            String question,
            int limit,
            User user,
            UUID documentId) {

        validateSearchInput(question, limit, user);

        // Verify document ownership before document-scoped retrieval.
        if (documentId != null) {

            Document document = documentRepository
                    .findById(documentId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Document not found"
                            )
                    );

            if (document.getUploadedBy() == null
                    || document.getUploadedBy().getId() == null
                    || !document.getUploadedBy()
                    .getId()
                    .equals(user.getId())) {

                throw new AccessDeniedException(
                        "You do not have access to this document"
                );
            }
        }

        // Generate an embedding for the user's question.
        List<Double> embedding =
                embeddingService.generateEmbedding(question);

        if (embedding == null || embedding.size() != 384) {
            int actualDimensions = embedding == null ? 0 : embedding.size();
            throw new IllegalStateException(
                    "Question embedding dimension mismatch: expected 384 but received "
                            + actualDimensions
            );
        }
        if (embedding.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalStateException(
                    "Question embedding contains a null or non-finite value"
            );
        }

        String vector = "[" +
                embedding.stream()
                        .map(Objects::toString)
                        .collect(Collectors.joining(",")) +
                "]";

        log.info(
                "Starting RAG search. documentId={}, limit={}, maxDistance={}, embeddingDimensions={}",
                documentId,
                limit,
                maxDistance,
                embedding.size()
        );


        List<SimilarChunkProjection> diagnosticResults;

        if (documentId == null) {
            diagnosticResults =
                    documentChunkRepository.findNearestChunksForDiagnostics(
                            vector,
                            10,
                            user.getId()
                    );
        } else {
            diagnosticResults =
                    documentChunkRepository.findNearestChunksForDiagnosticsByDocument(
                            vector,
                            10,
                            user.getId(),
                            documentId
                    );
        }

        log.info(
                "[RAG-DIAGNOSTIC] Nearest 10 chunks before threshold. " +
                        "question='{}', maxDistance={}",
                question,
                maxDistance
        );


        for (int i = 0; i < diagnosticResults.size(); i++) {
            SimilarChunkProjection chunk = diagnosticResults.get(i);

            String content = chunk.getContent();

            if (content != null && content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }

            log.info(
                    "[RAG-DIAGNOSTIC] rank={}, chunkId={}, page={}, " +
                            "index={}, distance={}, withinThreshold={}, " +
                            "documentName={}, content={}",
                    i + 1,
                    chunk.getChunkId(),
                    chunk.getPageNumber(),
                    chunk.getChunkIndex(),
                    chunk.getDistance(),
                    chunk.getDistance() != null
                            && chunk.getDistance() <= maxDistance,
                    chunk.getDocumentName(),
                    content
            );
        }


// Run the original, threshold-filtered search unchanged.
        List<SimilarChunkProjection> results;

        if (documentId == null) {
            results = documentChunkRepository.findSimilarChunks(
                    vector,
                    maxDistance,
                    limit,
                    user.getId()
            );
        } else {
            results = documentChunkRepository.findSimilarChunksByDocument(
                    vector,
                    maxDistance,
                    limit,
                    user.getId(),
                    documentId
            );
        }

        logRetrievedChunks(question, results);

        return results;

    }

    /**
     * Logs the content and similarity distance of retrieved chunks.
     * Intended for local debugging.
     */
    private void logRetrievedChunks(
            String question,
            List<SimilarChunkProjection> results) {

        log.info("RAG question: {}", question);
        log.info("RAG max distance: {}", maxDistance);
        log.info("RAG retrieved {} chunks", results.size());

        for (SimilarChunkProjection chunk : results) {

            String content = chunk.getContent();

            if (content != null && content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }

            log.info(
                    "[RAG-DIAGNOSTIC] chunkId={}, page={}, index={}, " +
                            "distance={}, withinThreshold={}, documentName={}, content={}",
                    chunk.getChunkId(),
                    chunk.getPageNumber(),
                    chunk.getChunkIndex(),
                    chunk.getDistance(),
                    chunk.getDistance() != null
                            && chunk.getDistance() <= maxDistance,
                    chunk.getDocumentName(),
                    content
            );
        }

        if (results.isEmpty()) {
            log.warn(
                    "No chunks passed vector-search filters for question: {}",
                    question
            );
        }
    }

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new IllegalStateException(
                    "Authenticated user is required for search"
            );
        }

        return userService.findByUsername(
                authentication.getName()
        );
    }

    private void validateSearchInput(
            String question,
            int limit,
            User user) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Search question must not be blank"
            );
        }

        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "Search limit must be greater than zero"
            );
        }

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "A valid user is required for search"
            );
        }
    }
}
