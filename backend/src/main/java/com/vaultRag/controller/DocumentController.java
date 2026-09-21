package com.vaultRag.backend.controller;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.service.AuditLogService;
import com.vaultRag.backend.service.DocumentAccessService;
import com.vaultRag.backend.service.DocumentService;
import com.vaultRag.backend.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;
    private final DocumentAccessService documentAccessService;
    private final AuditLogService auditLogService;

    public DocumentController(
            DocumentService documentService,
            UserService userService,
            DocumentAccessService documentAccessService,
            AuditLogService auditLogService) {

        this.documentService = documentService;
        this.userService = userService;
        this.documentAccessService = documentAccessService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(
                    authentication.getName()
            );

            Document document =
                    documentService.uploadDocument(file, user);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "id", document.getId(),
                            "name", document.getName(),
                            "fileType", document.getFileType(),
                            "fileSize", document.getFileSize(),
                            "status", document.getStatus()
                    ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", e.getMessage()
                    ));

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to upload document"
                    ));
        }
    }

    @GetMapping
    public ResponseEntity<?> getDocuments(
            Authentication authentication) {

        User user = userService.findByUsername(
                authentication.getName()
        );

        var documents =
                documentService.getDocumentsForUser(user);

        var response = documents.stream()
                .map(document -> Map.of(
                        "id", document.getId(),
                        "name", document.getName(),
                        "fileType", document.getFileType(),
                        "fileSize", document.getFileSize(),
                        "status", document.getStatus(),
                        "createdAt", document.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(
            @PathVariable UUID id,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(
                    authentication.getName()
            );

            Document document =
                    documentService.getDocumentById(id);

            boolean canAccess =
                    documentAccessService.canAccess(
                            document,
                            user.getUsername()
                    );

            if (!canAccess) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error",
                                "You do not have access to this document"
                        ));
            }

            auditLogService.log(
                    user,
                    "DOCUMENT_ACCESSED",
                    "DOCUMENT",
                    document.getId()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "id", document.getId(),
                            "name", document.getName(),
                            "fileType", document.getFileType(),
                            "fileSize", document.getFileSize(),
                            "status", document.getStatus(),
                            "createdAt", document.getCreatedAt()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }
}