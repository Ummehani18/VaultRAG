package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentChunkRepository;
import com.vaultRag.backend.repository.SimilarChunkProjection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final UserService userService;

    @Value("${vaultrag.rag.max-distance}")
    private double maxDistance;

    public SearchService(
            EmbeddingService embeddingService,
            DocumentChunkRepository documentChunkRepository,
            UserService userService) {

        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
        this.userService = userService;
    }

    public List<SimilarChunkProjection> search(
            String question,
            int limit) {

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

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new IllegalStateException(
                    "Authenticated user is required for search"
            );
        }

        String username = authentication.getName();

        User user = userService.findByUsername(username);

        List<Double> embedding =
                embeddingService.generateEmbedding(question);

        String vector =
                "[" +
                        embedding.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")) +
                        "]";

        return documentChunkRepository.findSimilarChunks(
                vector,
                maxDistance,
                limit,
                user.getId()
        );
    }
}