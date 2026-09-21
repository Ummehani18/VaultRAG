package com.vaultRag.backend.dto;

import java.util.UUID;

public record RagSource(
        UUID chunkId,
        UUID documentId,
        String documentName,
        int chunkIndex,
        Integer pageNumber,
        String content
) {
}