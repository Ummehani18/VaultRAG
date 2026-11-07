package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.DocumentChunk;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentExtractionService extractionService;
    private final DocumentChunkingService chunkingService;
    private final DocumentChunkService documentChunkService;
    private final AuditLogService auditLogService;
    private final Path storageDirectory;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentExtractionService extractionService,
            DocumentChunkingService chunkingService,
            DocumentChunkService documentChunkService,
            AuditLogService auditLogService,
            @Value("${vaultrag.storage.documents:storage/documents}") String storagePath) {

        this.documentRepository = documentRepository;
        this.extractionService = extractionService;
        this.chunkingService = chunkingService;
        this.documentChunkService = documentChunkService;
        this.auditLogService = auditLogService;
        this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public List<Document> getDocumentsForUser(User user) {
        return documentRepository.findByUploadedById(user.getId());
    }

    public Document uploadDocument(MultipartFile file, User user) throws Exception {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("An authenticated user is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String contentType = file.getContentType();
        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        Files.createDirectories(storageDirectory);

        UUID documentId = UUID.randomUUID();
        Path targetPath = storageDirectory.resolve(documentId + ".pdf").normalize();
        if (!targetPath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage path");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath);
        } catch (Exception e) {
            Files.deleteIfExists(targetPath);
            throw e;
        }

        Document document = new Document();
        document.setName(originalFilename);
        document.setFileType(contentType);
        document.setFileSize(file.getSize());
        document.setStoragePath(targetPath.toString());
        document.setUploadedBy(user);
        document.setStatus("UPLOADED");

        Document savedDocument;
        try {
            savedDocument = documentRepository.save(document);
        } catch (Exception e) {
            Files.deleteIfExists(targetPath);
            throw e;
        }

        auditLogService.log(user, "DOCUMENT_UPLOADED", "DOCUMENT", savedDocument.getId());

        try {
            log.info("Starting document ingestion: documentId={}, filename={}, sizeBytes={}",
                    savedDocument.getId(), originalFilename, file.getSize());

            List<ExtractedPage> pages = extractionService.extractPages(targetPath);
            if (pages == null || pages.isEmpty()) {
                throw new IllegalStateException(
                        "No readable text was extracted from the PDF. It may be scanned or image-only; OCR is not currently available.");
            }
            log.info("PDF text extraction completed: documentId={}, extractedPages={}",
                    savedDocument.getId(), pages.size());

            List<PageChunk> chunks = chunkingService.chunkPages(pages);
            if (chunks == null || chunks.isEmpty()) {
                throw new IllegalStateException("PDF text extraction succeeded, but no usable text chunks were created.");
            }
            log.info("Document chunking completed: documentId={}, chunks={}",
                    savedDocument.getId(), chunks.size());

            List<DocumentChunk> savedChunks = documentChunkService.saveChunks(savedDocument, chunks);
            if (savedChunks == null || savedChunks.size() != chunks.size()) {
                throw new IllegalStateException("Not all document chunks were saved.");
            }
            log.info("Document chunks saved: documentId={}, savedChunks={}",
                    savedDocument.getId(), savedChunks.size());

            for (DocumentChunk chunk : savedChunks) {
                if (chunk == null || chunk.getId() == null) {
                    throw new IllegalStateException("A saved document chunk has no database ID.");
                }
                documentChunkService.generateAndSaveEmbedding(chunk);
            }

            savedDocument.setStatus("READY");
            Document readyDocument = documentRepository.save(savedDocument);
            log.info("Document ingestion completed: documentId={}, chunks={}",
                    readyDocument.getId(), savedChunks.size());
            return readyDocument;

        } catch (Exception e) {
            log.error("Document ingestion failed: documentId={}, filename={}",
                    savedDocument.getId(), originalFilename, e);
            savedDocument.setStatus("FAILED");
            try {
                documentRepository.save(savedDocument);
            } catch (Exception statusException) {
                e.addSuppressed(statusException);
                log.error("Could not persist FAILED status for documentId={}",
                        savedDocument.getId(), statusException);
            }
            throw e;
        }
    }

    public Document getDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    /**
     * Deletes a document only when it belongs to the authenticated user.
     * The configured storage directory is the only permitted deletion location.
     */
    public void deleteDocument(UUID documentId, User user) throws Exception {
        Document document = getDocumentById(documentId);

        if (user == null || document.getUploadedBy() == null
                || document.getUploadedBy().getId() == null
                || !document.getUploadedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this document");
        }

        String storagePath = document.getStoragePath();
        if (storagePath != null && !storagePath.isBlank()) {
            Path filePath = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!filePath.startsWith(storageDirectory)) {
                throw new IllegalArgumentException("Invalid document storage path");
            }
            Files.deleteIfExists(filePath);
        }

        documentRepository.delete(document);
        auditLogService.log(user, "DOCUMENT_DELETED", "DOCUMENT", documentId);
    }

    public Document getDocumentByIdForUser(UUID documentId, User user) {
        Document document = getDocumentById(documentId);

        if (user == null || document.getUploadedBy() == null
                || document.getUploadedBy().getId() == null
                || !document.getUploadedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this document");
        }

        return document;
    }
}
