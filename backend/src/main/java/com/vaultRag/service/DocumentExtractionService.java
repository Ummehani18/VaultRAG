package com.vaultRag.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentExtractionService {

    private final Tika tika = new Tika();

    /**
     * Extracts the complete document text.
     *
     * This method is retained for general document extraction.
     */
    public String extractText(Path filePath)
            throws IOException, TikaException {

        validateFile(filePath);

        try (InputStream inputStream =
                     Files.newInputStream(filePath)) {

            return tika.parseToString(inputStream);
        }
    }

    /**
     * Extracts PDF text page-by-page.
     *
     * IMPORTANT:
     *
     * pageNumber is the physical PDF page number, starting from 1.
     *
     * This does NOT depend on whether the PDF itself contains
     * a printed page number.
     *
     * Example:
     *
     * PDF page 1 with no printed number
     *      -> pageNumber = 1
     *
     * PDF page 7 with no printed number
     *      -> pageNumber = 7
     *
     * This gives VaultRAG a reliable citation location even
     * when the original document has no visible page numbering.
     */
    public List<ExtractedPage> extractPages(Path filePath)
            throws IOException {

        validateFile(filePath);

        List<ExtractedPage> pages =
                new ArrayList<>();

        try (PDDocument document =
                     Loader.loadPDF(filePath.toFile())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            int totalPages =
                    document.getNumberOfPages();

            for (
                    int physicalPageNumber = 1;
                    physicalPageNumber <= totalPages;
                    physicalPageNumber++
            ) {

                stripper.setStartPage(
                        physicalPageNumber
                );

                stripper.setEndPage(
                        physicalPageNumber
                );

                String text =
                        stripper.getText(document)
                                .replaceAll("\\s+", " ")
                                .trim();

                /*
                 * Keep the physical PDF page number.
                 *
                 * We intentionally do not try to extract
                 * a printed page number from the text.
                 */
                if (!text.isBlank()) {

                    pages.add(
                            new ExtractedPage(
                                    physicalPageNumber,
                                    text
                            )
                    );
                }
            }
        }

        return pages;
    }

    private void validateFile(Path filePath) {

        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Document file path cannot be null"
            );
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "Document file not found"
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(
                    "Document path is not a file"
            );
        }
    }
}