package com.vaultRag.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentExtractionServiceTest {

    @Autowired
    private DocumentExtractionService extractionService;

    @Test
    void shouldExtractTextFromPdf() throws Exception {

        Path pdfPath = Path.of(
                "src/test/resources/test-policy.pdf"
        );

        String extractedText =
                extractionService.extractText(pdfPath);

        assertNotNull(extractedText);
        assertFalse(extractedText.isBlank());

        System.out.println(
                "Extracted text:\n" + extractedText
        );
    }
}