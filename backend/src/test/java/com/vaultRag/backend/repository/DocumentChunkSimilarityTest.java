package com.vaultRag.backend.repository;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentChunkSimilarityTest {

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${vaultrag.rag.max-distance}")
    private double maxDistance;

    @Test
    void shouldReturnResultsForRelevantQuestion() {

        String question =
                "What documents are required for a claim?";

        List<SimilarChunkProjection> results =
                search(question);

        assertNotNull(results);

        assertFalse(
                results.isEmpty(),
                "Expected relevant chunks to be returned"
        );

        assertTrue(results.size() <= 5);

        for (SimilarChunkProjection result : results) {

            assertNotNull(result.getDistance());

            assertTrue(
                    result.getDistance() <= maxDistance,
                    "Returned result exceeded similarity threshold"
            );
        }

        System.out.println(
                "\n========== RELEVANT QUESTION =========="
        );

        System.out.println(
                "Question: " + question
        );

        System.out.println(
                "Max distance: " + maxDistance
        );

        for (SimilarChunkProjection result : results) {

            System.out.println(
                    "Distance: " + result.getDistance()
            );

            System.out.println(
                    "Document: " + result.getDocumentName()
            );

            System.out.println(
                    "Chunk: " + result.getChunkIndex()
            );

            System.out.println(
                    "Page: " + result.getPageNumber()
            );
        }
    }

    @Test
    void shouldRejectUnrelatedQuestion() {

        String question =
                "What is the company's vacation policy?";

        List<SimilarChunkProjection> results =
                search(question);

        assertNotNull(results);

        assertTrue(
                results.isEmpty(),
                "Expected no results for an unrelated question"
        );

        System.out.println(
                "\n========== UNRELATED QUESTION =========="
        );

        System.out.println(
                "Question: " + question
        );

        System.out.println(
                "Max distance: " + maxDistance
        );

        System.out.println(
                "Results: " + results.size()
        );
    }

    private List<SimilarChunkProjection> search(
            String question) {

        List<Double> embedding =
                embeddingService.generateEmbedding(question);

        String vector =
                "[" +
                        embedding.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")) +
                        "]";

        Document document =
                documentRepository.findAll()
                        .stream()
                        .filter(existingDocument ->
                                "VaultRAG_3_Page_Insurance_Test.pdf"
                                        .equals(existingDocument.getName())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Expected VaultRAG_3_Page_Insurance_Test.pdf"
                                ));

        assertNotNull(
                document.getUploadedBy(),
                "Expected test document to have an owner"
        );

        return documentChunkRepository.findSimilarChunks(
                vector,
                maxDistance,
                5,
                document.getUploadedBy().getId()
        );
    }
}