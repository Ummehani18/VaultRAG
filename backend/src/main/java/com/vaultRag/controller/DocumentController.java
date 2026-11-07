
package com.vaultRag.backend.controller;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.service.AuditLogService;
import com.vaultRag.backend.service.DocumentAccessService;
import com.vaultRag.backend.service.DocumentFileService;
import com.vaultRag.backend.service.DocumentService;
import com.vaultRag.backend.service.UserService;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final AuditLogService auditLogService;
    private final DocumentFileService documentFileService;

    public DocumentController(
            DocumentService documentService,
            UserService userService,
            DocumentAccessService documentAccessService,
            DocumentFileService documentFileService,
            AuditLogService auditLogService) {

        this.documentService = documentService;
        this.userService = userService;
        this.documentFileService = documentFileService;
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
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload document"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getDocuments(
            Authentication authentication) {

        User user = userService.findByUsername(
                authentication.getName()
        );

        var documents = documentService.getDocumentsForUser(user);

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

            // Verify ownership before returning document details.
            Document document =
                    documentService.getDocumentByIdForUser(id, user);

            // Only log access after ownership is verified.
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

        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "You do not have access to this document"
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable UUID id,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(
                    authentication.getName()
            );

            documentService.deleteDocument(id, user);

            return ResponseEntity.noContent().build();

        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "You do not have access to this document"
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete document"));
        }
    }

    @GetMapping(
            value = "/{id}/file",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<?> getDocumentFile(
            @PathVariable UUID id,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(
                    authentication.getName()
            );

            Resource resource =
                    documentFileService.getDocumentFile(id, user);

            auditLogService.log(
                    user,
                    "DOCUMENT_FILE_ACCESSED",
                    "DOCUMENT",
                    id
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" +
                                    resource.getFilename() + "\""
                    )
                    .body(resource);

        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "You do not have access to this document"
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
