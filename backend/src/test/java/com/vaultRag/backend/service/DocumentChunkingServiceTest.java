package com.vaultRag.backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkingServiceTest {

    private final DocumentChunkingService chunkingService =
            new DocumentChunkingService();

    @Test
    void shouldReturnEmptyListForBlankText() {

        List<String> chunks =
                chunkingService.chunkText("   ");

        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldReturnOneChunkForSmallText() {

        String text =
                "This is a small insurance policy document.";

        List<String> chunks =
                chunkingService.chunkText(text);

        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void shouldSplitLargeTextIntoMultipleChunks() {

        String text = "word ".repeat(300);

        List<String> chunks =
                chunkingService.chunkText(text);

        assertTrue(chunks.size() > 1);

        for (String chunk : chunks) {
            assertFalse(chunk.isBlank());
            assertTrue(chunk.length() <= 500);
        }
    }

    @Test
    void shouldPreserveChunkOrder() {

        String text =
                "START " +
                        "A ".repeat(150) +
                        "MIDDLE " +
                        "B ".repeat(150) +
                        "END";

        List<String> chunks =
                chunkingService.chunkText(text);

        assertFalse(chunks.isEmpty());

        assertTrue(
                chunks.get(0).startsWith("START")
        );

        assertTrue(
                chunks.get(chunks.size() - 1).contains("END")
        );
    }

    @Test
    void shouldPreservePageNumberWhenChunkingPages() {

        List<ExtractedPage> pages =
                List.of(
                        new ExtractedPage(
                                1,
                                "Pre-authorization is required for planned hospital admissions."
                        ),
                        new ExtractedPage(
                                2,
                                "A standard claim submission should include the claim form and hospital bill."
                        )
                );

        List<PageChunk> chunks =
                chunkingService.chunkPages(pages);

        assertNotNull(chunks);

        assertFalse(chunks.isEmpty());

        assertEquals(1, chunks.get(0).pageNumber());

        assertEquals(0, chunks.get(0).chunkIndex());

        assertEquals(2, chunks.get(1).pageNumber());

        assertEquals(1, chunks.get(1).chunkIndex());

        System.out.println(
                "\n========== PAGE CHUNKS =========="
        );

        for (PageChunk chunk : chunks) {

            System.out.println(
                    "Page: " + chunk.pageNumber()
                            + " | Chunk: "
                            + chunk.chunkIndex()
            );

            System.out.println(
                    "Content: " + chunk.content()
            );
        }
    }
}