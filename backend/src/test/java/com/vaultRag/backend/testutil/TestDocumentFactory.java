package com.vaultRag.backend.testutil;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.service.DocumentService;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;

@Component
public class TestDocumentFactory {

    private final DocumentService documentService;

    public TestDocumentFactory(DocumentService documentService) {
        this.documentService = documentService;
    }

    public Document createInsuranceDocument(User user) throws Exception {

        ClassPathResource resource =
                new ClassPathResource(
                        "VaultRAG_Healthcare_Insurance_Test_Dataset.pdf"
                );

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Test insurance PDF not found"
            );
        }

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "VaultRAG_Healthcare_Insurance_Test_Dataset.pdf",
                        "application/pdf",
                        resource.getInputStream()
                );

        return documentService.uploadDocument(
                file,
                user
        );
    }
}