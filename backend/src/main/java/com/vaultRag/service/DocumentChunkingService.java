package com.vaultRag.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;

    public List<String> chunkText(String text) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalizedText = text
                .replaceAll("\\s+", " ")
                .trim();

        if (normalizedText.length() <= CHUNK_SIZE) {
            return List.of(normalizedText);
        }

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < normalizedText.length()) {

            int end = Math.min(
                    start + CHUNK_SIZE,
                    normalizedText.length()
            );

            if (end < normalizedText.length()) {

                int lastSpace =
                        normalizedText.lastIndexOf(" ", end);

                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            String chunk =
                    normalizedText.substring(start, end).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= normalizedText.length()) {
                break;
            }

            start = end - CHUNK_OVERLAP;
        }

        return chunks;
    }

    public List<PageChunk> chunkPages(
            List<ExtractedPage> pages) {

        if (pages == null || pages.isEmpty()) {
            return List.of();
        }

        List<PageChunk> pageChunks =
                new ArrayList<>();

        int globalChunkIndex = 0;

        for (ExtractedPage page : pages) {

            List<String> chunks =
                    chunkText(page.text());

            for (String chunk : chunks) {

                pageChunks.add(
                        new PageChunk(
                                page.pageNumber(),
                                globalChunkIndex,
                                chunk
                        )
                );

                globalChunkIndex++;
            }
        }

        return pageChunks;
    }
}