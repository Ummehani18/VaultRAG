package com.vaultRag.backend.controller;

import com.vaultRag.backend.dto.RagResponse;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.service.AuditLogService;
import com.vaultRag.backend.service.RagService;
import com.vaultRag.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            @RequestParam(defaultValue = "3") int limit,
            Authentication authentication) {

        try {

            RagResponse response =
                    ragService.answer(
                            question,
                            limit
                    );

            User user = userService.findByUsername(
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
        }
    }
}