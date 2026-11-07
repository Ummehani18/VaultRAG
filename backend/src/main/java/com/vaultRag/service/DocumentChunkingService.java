package com.vaultRag.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;

    /**
     * Splits text into overlapping chunks, preferring word boundaries.
     * Every iteration advances the starting offset, preventing an infinite loop.
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalizedText = text.replaceAll("\\s+", " ").trim();
        if (normalizedText.isEmpty()) {
            return List.of();
        }

        if (normalizedText.length() <= CHUNK_SIZE) {
            return List.of(normalizedText);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalizedText.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalizedText.length());

            if (end < normalizedText.length()) {
                int lastSpace = normalizedText.lastIndexOf(' ', end - 1);

                // Only move the boundary when doing so still guarantees forward progress
                // after applying the configured overlap.
                if (lastSpace > start + CHUNK_OVERLAP) {
                    end = lastSpace;
                }
            }

            String chunk = normalizedText.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= normalizedText.length()) {
                break;
            }

            int nextStart = end - CHUNK_OVERLAP;
            if (nextStart <= start) {
                nextStart = start + 1;
            }
            start = nextStart;
        }

        return chunks;
    }

    /**
     * Chunks extracted PDF pages while preserving each physical PDF page number.
     */
    public List<PageChunk> chunkPages(List<ExtractedPage> pages) {
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }

        List<PageChunk> pageChunks = new ArrayList<>();
        int globalChunkIndex = 0;

        for (ExtractedPage page : pages) {
            if (page == null || page.text() == null || page.text().isBlank()) {
                continue;
            }

            List<String> chunks = chunkText(page.text());
            for (String chunk : chunks) {
                if (chunk == null || chunk.isBlank()) {
                    continue;
                }

                pageChunks.add(new PageChunk(
                        page.pageNumber(),
                        globalChunkIndex,
                        chunk
                ));
                globalChunkIndex++;
            }
        }

        return pageChunks;
    }
}
