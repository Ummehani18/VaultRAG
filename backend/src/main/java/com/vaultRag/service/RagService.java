package com.vaultRag.backend.service;

import com.vaultRag.backend.dto.RagResponse;
import com.vaultRag.backend.dto.RagSource;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.SimilarChunkProjection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class RagService {

    private static final Logger log =
            LoggerFactory.getLogger(RagService.class);

    private final SearchService searchService;
    private final LlmService llmService;

    public RagService(
            SearchService searchService,
            LlmService llmService) {

        this.searchService = searchService;
        this.llmService = llmService;
    }

    /*
     * Existing all-document behavior.
     *
     * Kept for backward compatibility.
     */
    public RagResponse answer(
            String question,
            int limit) {

        return answer(
                question,
                limit,
                null
        );
    }

    /*
     * New RAG method.
     *
     * documentId == null
     *     -> search all user's documents
     *
     * documentId != null
     *     -> search only selected document
     */
    public RagResponse answer(
            String question,
            int limit,
            UUID documentId) {

        long totalStart = System.nanoTime();

        log.info(
                "[RAG] Starting request: question='{}', limit={}, documentId={}",
                question,
                limit,
                documentId
        );

        // 1. Retrieval
        long searchStart = System.nanoTime();

        List<SimilarChunkProjection> chunks =
                searchService.search(
                        question,
                        limit,
                        documentId
                );

        long searchTime =
                (System.nanoTime() - searchStart)
                        / 1_000_000;

        log.info(
                "[RAG] Search completed: {} ms, chunks={}, documentId={}",
                searchTime,
                chunks.size(),
                documentId
        );

        if (chunks.isEmpty()) {

            long totalTime =
                    (System.nanoTime() - totalStart)
                            / 1_000_000;

            log.info(
                    "[RAG] No relevant chunks. Total time: {} ms",
                    totalTime
            );

            return new RagResponse(
                    "I could not find relevant information in the provided documents.",
                    List.of()
            );
        }

        // 2. Build context
        long contextStart = System.nanoTime();

        List<String> context =
                chunks.stream()
                        .map(SimilarChunkProjection::getContent)
                        .toList();

        long contextTime =
                (System.nanoTime() - contextStart)
                        / 1_000_000;

        log.info(
                "[RAG] Context built: {} ms, contextChunks={}",
                contextTime,
                context.size()
        );

        // 3. LLM generation
        long llmStart = System.nanoTime();

        String answer =
                llmService.generateAnswer(
                        question,
                        context
                );

        long llmTime =
                (System.nanoTime() - llmStart)
                        / 1_000_000;

        log.info(
                "[RAG] LLM generation completed: {} ms",
                llmTime
        );

        // 4. Build sources
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

        long totalTime =
                (System.nanoTime() - totalStart)
                        / 1_000_000;

        log.info(
                "[RAG] Request completed: total={} ms, search={} ms, llm={} ms",
                totalTime,
                searchTime,
                llmTime
        );

        return new RagResponse(
                answer,
                sources
        );
    }

    /*
     * Existing streaming behavior.
     *
     * Kept for compatibility.
     */
    public void streamAnswer(
            String question,
            int limit,
            User user,
            Consumer<List<RagSource>> onSources,
            Consumer<String> onChunk) {

        streamAnswer(
                question,
                limit,
                user,
                null,
                onSources,
                onChunk
        );
    }

    /*
     * New streaming RAG method with optional document scope.
     */
    public void streamAnswer(
            String question,
            int limit,
            User user,
            UUID documentId,
            Consumer<List<RagSource>> onSources,
            Consumer<String> onChunk) {

        log.info(
                "[RAG] Starting streaming request: question='{}', limit={}, documentId={}",
                question,
                limit,
                documentId
        );

        List<SimilarChunkProjection> chunks =
                searchService.search(
                        question,
                        limit,
                        user,
                        documentId
                );

        if (chunks.isEmpty()) {

            onSources.accept(List.of());

            onChunk.accept(
                    "I could not find relevant information in the provided documents."
            );

            return;
        }

        // Build source metadata
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

        // Send sources before generation
        onSources.accept(sources);

        List<String> context =
                chunks.stream()
                        .map(SimilarChunkProjection::getContent)
                        .toList();

        llmService.streamAnswer(
                question,
                context,
                onChunk
        );
    }
}