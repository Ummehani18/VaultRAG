package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.DocumentChunk;
import com.vaultRag.backend.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentChunkService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    public DocumentChunkService(
            DocumentChunkRepository documentChunkRepository,
            EmbeddingService embeddingService) {

        this.documentChunkRepository =
                documentChunkRepository;

        this.embeddingService =
                embeddingService;
    }

    public List<DocumentChunk> saveChunks(
            Document document,
            List<PageChunk> pageChunks) {

        if (pageChunks == null || pageChunks.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks =
                new java.util.ArrayList<>();

        for (PageChunk pageChunk : pageChunks) {

            DocumentChunk chunk =
                    new DocumentChunk();

            chunk.setDocument(document);

            chunk.setChunkIndex(
                    pageChunk.chunkIndex()
            );

            chunk.setPageNumber(
                    pageChunk.pageNumber()
            );

            chunk.setContent(
                    pageChunk.content()
            );

            chunks.add(chunk);
        }

        return documentChunkRepository.saveAll(chunks);
    }

    public void generateAndSaveEmbedding(
            DocumentChunk chunk) {

        List<Double> embedding =
                embeddingService.generateEmbedding(
                        chunk.getContent()
                );

        String vector =
                "[" +
                        embedding.stream()
                                .map(String::valueOf)
                                .collect(
                                        java.util.stream.Collectors.joining(",")
                                ) +
                        "]";

        documentChunkRepository.updateEmbedding(
                chunk.getId(),
                vector
        );
    }
}