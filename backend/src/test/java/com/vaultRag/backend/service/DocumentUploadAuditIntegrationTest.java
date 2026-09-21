package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.AuditLog;
import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.AuditLogRepository;
import com.vaultRag.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentUploadAuditIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    void shouldCreateAuditLogWhenDocumentIsUploaded() throws Exception {

        // Arrange
        String username = "document-audit-test-user";

        User user = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userService.createUser(
                                username,
                                "test-password-123",
                                "USER"
                        )
                );

        byte[] pdfContent = createMinimalPdf();

        MultipartFile file = new MockMultipartFile(
                "file",
                "audit-test.pdf",
                "application/pdf",
                pdfContent
        );

        // Act
        Document document =
                documentService.uploadDocument(file, user);

        // Assert
        assertNotNull(document);
        assertNotNull(document.getId());

        List<AuditLog> auditLogs =
                auditLogRepository.findAll()
                        .stream()
                        .filter(log ->
                                "DOCUMENT_UPLOADED".equals(log.getAction())
                                        && document.getId().equals(log.getResourceId())
                        )
                        .toList();

        assertEquals(1, auditLogs.size());

        AuditLog auditLog = auditLogs.get(0);

        assertNotNull(auditLog.getId());
        assertEquals(user.getId(), auditLog.getUser().getId());
        assertEquals("DOCUMENT_UPLOADED", auditLog.getAction());
        assertEquals("DOCUMENT", auditLog.getResourceType());
        assertEquals(document.getId(), auditLog.getResourceId());
        assertNotNull(auditLog.getCreatedAt());
    }

    private byte[] createMinimalPdf() {

        String pdf =
                "%PDF-1.4\n" +
                        "1 0 obj\n" +
                        "<< /Type /Catalog /Pages 2 0 R >>\n" +
                        "endobj\n" +
                        "2 0 obj\n" +
                        "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                        "endobj\n" +
                        "3 0 obj\n" +
                        "<< /Type /Page /Parent 2 0 R " +
                        "/MediaBox [0 0 612 792] " +
                        "/Contents 4 0 R " +
                        "/Resources << /Font << /F1 5 0 R >> >> >>\n" +
                        "endobj\n" +
                        "4 0 obj\n" +
                        "<< /Length 56 >>\n" +
                        "stream\n" +
                        "BT\n" +
                        "/F1 12 Tf\n" +
                        "72 720 Td\n" +
                        "(Audit test document) Tj\n" +
                        "ET\n" +
                        "endstream\n" +
                        "endobj\n" +
                        "5 0 obj\n" +
                        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n" +
                        "endobj\n" +
                        "xref\n" +
                        "0 6\n" +
                        "0000000000 65535 f \n" +
                        "0000000009 00000 n \n" +
                        "0000000058 00000 n \n" +
                        "0000000120 00000 n \n" +
                        "0000000275 00000 n \n" +
                        "0000000380 00000 n \n" +
                        "trailer\n" +
                        "<< /Size 6 /Root 1 0 R >>\n" +
                        "startxref\n" +
                        "455\n" +
                        "%%EOF";

        return pdf.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}