package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.DocumentChunk;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentChunkRepository;
import com.vaultRag.backend.repository.DocumentRepository;
import com.vaultRag.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentChunkEmbeddingTest {

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void shouldGenerateAndSaveEmbedding() {

        User user = new User();
        user.setUsername(
                "embedding-test-" + System.nanoTime()
        );
        user.setPasswordHash("test-password-hash");
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        Document document = new Document();
        document.setName("embedding-test.pdf");
        document.setFileType("application/pdf");
        document.setFileSize(100L);
        document.setStoragePath("/test/embedding-test.pdf");
        document.setUploadedBy(savedUser);
        document.setStatus("UPLOADED");

        Document savedDocument =
                documentRepository.save(document);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(savedDocument);
        chunk.setChunkIndex(0);
        chunk.setContent(
                "Pre-authorization is required for planned hospital admissions."
        );

        DocumentChunk savedChunk =
                documentChunkRepository.save(chunk);

        // Generate embedding and save it through the service
        documentChunkService.generateAndSaveEmbedding(
                savedChunk
        );

        // Verify that the embedding was actually persisted
        boolean hasEmbedding =
                documentChunkRepository.hasEmbedding(
                        savedChunk.getId()
                );

        assertTrue(hasEmbedding);
    }
}