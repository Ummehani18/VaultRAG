package com.vaultRag.backend.controller;

import com.vaultRag.backend.repository.SimilarChunkProjection;
import com.vaultRag.backend.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam String question,
            @RequestParam(defaultValue = "5") int limit) {

        try {

            List<SimilarChunkProjection> results =
                    searchService.search(
                            question,
                            limit
                    );

            List<Map<String, Object>> response =
                    results.stream()
                            .map(chunk -> Map.<String, Object>of(
                                    "chunkId",
                                    chunk.getChunkId(),

                                    "documentId",
                                    chunk.getDocumentId(),

                                    "documentName",
                                    chunk.getDocumentName(),

                                    "chunkIndex",
                                    chunk.getChunkIndex(),

                                    "distance",
                                    chunk.getDistance(),

                                    "content",
                                    chunk.getContent()
                            ))
                            .toList();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }
}