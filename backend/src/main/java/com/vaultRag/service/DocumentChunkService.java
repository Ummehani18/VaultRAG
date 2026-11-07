package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.DocumentChunk;
import com.vaultRag.backend.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DocumentChunkService {

    private static final int EXPECTED_EMBEDDING_DIMENSIONS = 384;

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    public DocumentChunkService(
            DocumentChunkRepository documentChunkRepository,
            EmbeddingService embeddingService) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
    }

    public List<DocumentChunk> saveChunks(Document document, List<PageChunk> pageChunks) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("A persisted document is required to save chunks");
        }
        if (pageChunks == null || pageChunks.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();

        for (PageChunk pageChunk : pageChunks) {
            if (pageChunk == null || pageChunk.content() == null || pageChunk.content().isBlank()) {
                continue;
            }

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(pageChunk.chunkIndex());
            chunk.setPageNumber(pageChunk.pageNumber());
            chunk.setContent(pageChunk.content().trim());
            chunks.add(chunk);
        }

        if (chunks.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> savedChunks = documentChunkRepository.saveAll(chunks);
        if (savedChunks == null || savedChunks.size() != chunks.size()) {
            throw new IllegalStateException("Not all document chunks were persisted.");
        }
        return savedChunks;
    }

    public void generateAndSaveEmbedding(DocumentChunk chunk) {
        if (chunk == null || chunk.getId() == null) {
            throw new IllegalArgumentException("A persisted document chunk is required for embedding");
        }
        if (chunk.getContent() == null || chunk.getContent().isBlank()) {
            throw new IllegalArgumentException("Cannot generate an embedding for empty chunk content");
        }

        List<Double> embedding = embeddingService.generateEmbedding(chunk.getContent());
        if (embedding == null || embedding.size() != EXPECTED_EMBEDDING_DIMENSIONS) {
            int actualDimensions = embedding == null ? 0 : embedding.size();
            throw new IllegalStateException(
                    "Embedding dimension mismatch: expected "
                            + EXPECTED_EMBEDDING_DIMENSIONS + " but received " + actualDimensions);
        }

        if (embedding.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalStateException("Embedding contains a null or non-finite value");
        }

        String vector = "[" + embedding.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(",")) + "]";

        int updatedRows = documentChunkRepository.updateEmbedding(chunk.getId(), vector);
        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Expected to update one embedding row for chunk " + chunk.getId()
                            + ", but updated " + updatedRows);
        }
        if (!documentChunkRepository.hasEmbedding(chunk.getId())) {
            throw new IllegalStateException(
                    "Embedding update did not persist for chunk " + chunk.getId());
        }
    }
}
