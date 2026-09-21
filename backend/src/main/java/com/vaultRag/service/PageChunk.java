package com.vaultRag.backend.service;

public record PageChunk(
        int pageNumber,
        int chunkIndex,
        String content
) {
}