package com.vaultRag.backend.service;

import com.vaultRag.backend.dto.RagResponse;
import com.vaultRag.backend.dto.RagSource;
import com.vaultRag.backend.repository.SimilarChunkProjection;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final SearchService searchService;
    private final LlmService llmService;

    public RagService(
            SearchService searchService,
            LlmService llmService) {

        this.searchService = searchService;
        this.llmService = llmService;
    }

    public RagResponse answer(
            String question,
            int limit) {

        List<SimilarChunkProjection> chunks =
                searchService.search(
                        question,
                        limit
                );

        if (chunks.isEmpty()) {

            return new RagResponse(
                    "I could not find relevant information in the provided documents.",
                    List.of()
            );
        }

        List<String> context =
                chunks.stream()
                        .map(SimilarChunkProjection::getContent)
                        .toList();

        String answer =
                llmService.generateAnswer(
                        question,
                        context
                );

        List<RagSource> sources =
                chunks.stream()
                        .map(chunk ->
                                new RagSource(
                                        chunk.getChunkId(),
                                        chunk.getDocumentId(),
                                        chunk.getDocumentName(),
                                        chunk.getChunkIndex(),
                                        chunk.getPageNumber(),
                                        chunk.getContent()
                                )
                        )
                        .toList();

        return new RagResponse(
                answer,
                sources
        );
    }
}