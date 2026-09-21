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

    public String extractText(Path filePath)
            throws IOException, TikaException {

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "Document file not found"
            );
        }

        try (InputStream inputStream =
                     Files.newInputStream(filePath)) {

            return tika.parseToString(inputStream);
        }
    }

    public List<ExtractedPage> extractPages(Path filePath)
            throws IOException {

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "Document file not found"
            );
        }

        List<ExtractedPage> pages = new ArrayList<>();

        try (PDDocument document =
                     Loader.loadPDF(filePath.toFile())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            int totalPages = document.getNumberOfPages();

            for (int pageNumber = 1;
                 pageNumber <= totalPages;
                 pageNumber++) {

                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);

                String text =
                        stripper.getText(document)
                                .replaceAll("\\s+", " ")
                                .trim();

                if (!text.isBlank()) {
                    pages.add(
                            new ExtractedPage(
                                    pageNumber,
                                    text
                            )
                    );
                }
            }
        }

        return pages;
    }
}