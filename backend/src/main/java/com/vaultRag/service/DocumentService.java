package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.vaultRag.backend.entity.DocumentChunk;

import com.vaultRag.backend.service.AuditLogService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentExtractionService extractionService;
    private final DocumentChunkingService chunkingService;
    private final DocumentChunkService documentChunkService;

    private final Path storageDirectory;

    private final AuditLogService auditLogService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentExtractionService extractionService,
            DocumentChunkingService chunkingService,
            DocumentChunkService documentChunkService,
            AuditLogService auditLogService,
            @Value("${vaultrag.storage.documents:storage/documents}")
            String storagePath) {

        this.documentRepository = documentRepository;
        this.extractionService = extractionService;
        this.chunkingService = chunkingService;
        this.documentChunkService = documentChunkService;
        this.auditLogService = auditLogService;

        this.storageDirectory = Paths.get(storagePath)
                .toAbsolutePath()
                .normalize();
    }

    public List<Document> getDocumentsForUser(User user) {

        return documentRepository
                .findByUploadedById(user.getId());
    }

    public Document uploadDocument(
            MultipartFile file,
            User user) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File must not be empty"
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                originalFilename.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid filename"
            );
        }

        String contentType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException(
                    "Only PDF files are supported"
            );
        }

        Files.createDirectories(storageDirectory);

        UUID documentId = UUID.randomUUID();

        String storedFilename =
                documentId + ".pdf";

        Path targetPath =
                storageDirectory
                        .resolve(storedFilename)
                        .normalize();

        if (!targetPath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid storage path"
            );
        }

        Files.copy(
                file.getInputStream(),
                targetPath
        );

        Document document = new Document();

        document.setName(originalFilename);
        document.setFileType(contentType);
        document.setFileSize(file.getSize());
        document.setStoragePath(targetPath.toString());
        document.setUploadedBy(user);
        document.setStatus("UPLOADED");

        Document savedDocument =
                documentRepository.save(document);

        auditLogService.log(
                user,
                "DOCUMENT_UPLOADED",
                "DOCUMENT",
                savedDocument.getId()
        );

        // Extract text from the PDF
        List<ExtractedPage> pages =
                extractionService.extractPages(targetPath);

        List<PageChunk> chunks =
                chunkingService.chunkPages(pages);

        List<DocumentChunk> savedChunks =
                documentChunkService.saveChunks(
                        savedDocument,
                        chunks
                );

// Generate and save an embedding for each chunk
        for (DocumentChunk chunk : savedChunks) {
            documentChunkService.generateAndSaveEmbedding(chunk);
        }

        return savedDocument;
    }
    public Document getDocumentById(UUID documentId) {

        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found"
                        )
                );
    }
}