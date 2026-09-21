package com.vaultRag.backend.dto;

import java.util.List;

public record RagResponse(
        String answer,
        List<RagSource> sources
) {
}