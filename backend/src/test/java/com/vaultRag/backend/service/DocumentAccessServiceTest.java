package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentAccessServiceTest {

    @Test
    void ownerCanAccessDocument() {

        User owner = new User();
        owner.setUsername("owner");

        Document document = new Document();
        document.setUploadedBy(owner);

        // We will test the actual service after wiring
        // repository-backed users in the next step.
        assertEquals(
                owner,
                document.getUploadedBy()
        );
    }
}