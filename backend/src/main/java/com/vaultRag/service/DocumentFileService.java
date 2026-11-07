package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class DocumentFileService {

    private final DocumentService documentService;
    private final DocumentAccessService documentAccessService;
    private final Path documentsRoot;

    public DocumentFileService(
            DocumentService documentService,
            DocumentAccessService documentAccessService,
            @Value("${vaultrag.storage.documents:storage/documents}")
            String documentsPath) {

        this.documentService = documentService;
        this.documentAccessService = documentAccessService;

        this.documentsRoot =
                Paths.get(documentsPath)
                        .toAbsolutePath()
                        .normalize();
    }

    public Resource getDocumentFile(
            UUID documentId,
            User user) {

        if (user == null) {
            throw new AccessDeniedException(
                    "Authenticated user is required"
            );
        }

        Document document =
                documentService.getDocumentByIdForUser(
                        documentId,
                        user
                );

        String storagePath =
                document.getStoragePath();

        if (storagePath == null ||
                storagePath.isBlank()) {

            throw new IllegalArgumentException(
                    "Document file path is missing"
            );
        }

        Path filePath =
                Paths.get(storagePath)
                        .toAbsolutePath()
                        .normalize();

        /*
         * Security check:
         *
         * The resolved document must remain inside
         * VaultRAG's document storage directory.
         *
         * This prevents paths such as:
         *
         * ../../some-secret-file.pdf
         */
        if (!filePath.startsWith(documentsRoot)) {
            throw new AccessDeniedException(
                    "Invalid document storage path"
            );
        }

        if (!Files.exists(filePath) ||
                !Files.isRegularFile(filePath)) {

            throw new IllegalArgumentException(
                    "Document file not found"
            );
        }

        if (!filePath.toString()
                .toLowerCase()
                .endsWith(".pdf")) {

            throw new IllegalArgumentException(
                    "Only PDF documents can be viewed"
            );
        }

        if (!filePath.startsWith(documentsRoot)) {
            throw new AccessDeniedException(
                    "Invalid document storage path"
            );
        }

        return new FileSystemResource(filePath);
    }
}