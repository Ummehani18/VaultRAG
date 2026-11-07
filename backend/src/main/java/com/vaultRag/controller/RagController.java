package com.vaultRag.backend.controller;

import com.vaultRag.backend.dto.RagResponse;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.service.AuditLogService;
import com.vaultRag.backend.service.RagService;
import com.vaultRag.backend.service.UserService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public RagController(
            RagService ragService,
            AuditLogService auditLogService,
            UserService userService) {

        this.ragService = ragService;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> answer(
            @RequestParam String question,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(required = false) UUID documentId,
            Authentication authentication) {

        try {

            RagResponse response =
                    ragService.answer(
                            question,
                            limit,
                            documentId
                    );

            User user =
                    userService.findByUsername(
                            authentication.getName()
                    );

            auditLogService.log(
                    user,
                    "RAG_QUERY_EXECUTED",
                    "RAG",
                    null
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));

        } catch (AccessDeniedException e) {

            return ResponseEntity
                    .status(403)
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(
            @RequestParam String question,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(required = false) UUID documentId,
            Authentication authentication) {

        User user =
                userService.findByUsername(
                        authentication.getName()
                );

        SseEmitter emitter =
                new SseEmitter(120_000L);

        emitter.onTimeout(
                emitter::complete
        );

        emitter.onError(error -> {
            emitter.completeWithError(error);
        });

        CompletableFuture.runAsync(() -> {

            try {

                ragService.streamAnswer(
                        question,
                        limit,
                        user,
                        documentId,

                        sources -> {

                            try {

                                emitter.send(
                                        SseEmitter.event()
                                                .name("sources")
                                                .data(sources)
                                );

                            } catch (Exception e) {

                                emitter.completeWithError(e);
                            }
                        },

                        chunk -> {

                            try {

                                emitter.send(
                                        SseEmitter.event()
                                                .name("answer")
                                                .data(chunk)
                                );

                            } catch (Exception e) {

                                emitter.completeWithError(e);
                            }
                        }
                );

                auditLogService.log(
                        user,
                        "RAG_QUERY_EXECUTED",
                        "RAG",
                        null
                );

                emitter.complete();

            } catch (Exception e) {

                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}