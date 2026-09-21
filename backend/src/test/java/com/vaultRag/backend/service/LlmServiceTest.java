package com.vaultRag.backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlmServiceTest {

    private final LlmService llmService =
            new LlmService(
                    "http://localhost:11434",
                    "llama3:latest"
            );

    @Test
    void shouldGenerateAnswerFromContext() {

        String question =
                "What documents are required for a claim?";

        List<String> context = List.of(
                """
                A standard claim submission should include
                the claim form, hospital bill, discharge summary,
                itemized treatment bill, payment receipt,
                and relevant medical reports.
                """
        );

        String answer =
                llmService.generateAnswer(
                        question,
                        context
                );

        assertNotNull(answer);
        assertFalse(answer.isBlank());

        System.out.println(
                "\nGenerated answer:\n" + answer
        );
    }
}