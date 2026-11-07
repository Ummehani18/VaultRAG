package com.vaultRag.backend.dto;

import java.util.UUID;

public record RagSource(
        UUID chunkId,
        UUID documentId,
        String documentName,
        int chunkIndex,

        /*
         * Physical PDF page number.
         *
         * This will be populated even when the
         * original PDF does not contain printed
         * page numbers.
         */
        Integer pageNumber,

        String content
) {
}