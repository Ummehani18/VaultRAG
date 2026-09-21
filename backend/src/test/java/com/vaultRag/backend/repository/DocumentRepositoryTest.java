package com.vaultRag.backend.repository;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindDocumentByUploadedUser() {

        User user = new User();
        user.setUsername("document-test-" + System.nanoTime());
        user.setPasswordHash("test-password-hash");
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        Document document = new Document();
        document.setName("test-policy.pdf");
        document.setFileType("application/pdf");
        document.setFileSize(1024L);
        document.setStoragePath("/data/test-policy.pdf");
        document.setUploadedBy(savedUser);
        document.setStatus("UPLOADED");

        Document savedDocument = documentRepository.save(document);

        assertNotNull(savedDocument.getId());
        assertNotNull(savedDocument.getCreatedAt());

        var documents =
                documentRepository.findByUploadedById(savedUser.getId());

        assertFalse(documents.isEmpty());
        assertEquals(savedDocument.getId(), documents.get(0).getId());
        assertEquals("test-policy.pdf", documents.get(0).getName());
    }
}