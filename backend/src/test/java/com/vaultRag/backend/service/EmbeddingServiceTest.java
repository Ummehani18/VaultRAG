package com.vaultRag.backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private final EmbeddingService embeddingService =
            new EmbeddingService(
                    "http://localhost:11434",
                    "all-minilm"
            );

    @Test
    void shouldGenerate384DimensionalEmbedding() {

        List<Double> embedding =
                embeddingService.generateEmbedding(
                        "Pre-authorization is required for planned hospital admissions."
                );

        assertNotNull(embedding);
        assertEquals(384, embedding.size());

        System.out.println(
                "Embedding dimensions: " + embedding.size()
        );

        System.out.println(
                "First 5 values: " +
                        embedding.subList(0, 5)
        );
    }
}