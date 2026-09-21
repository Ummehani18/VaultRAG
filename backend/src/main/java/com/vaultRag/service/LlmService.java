package com.vaultRag.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private final RestClient restClient;
    private final String generationModel;

    public LlmService(
            @Value("${vaultrag.ollama.base-url}")
            String baseUrl,
            @Value("${vaultrag.ollama.generation-model}")
            String generationModel) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.generationModel = generationModel;
    }

    public String generateAnswer(
            String question,
            List<String> context) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question must not be blank"
            );
        }

        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException(
                    "Context must not be empty"
            );
        }

        String contextText =
                String.join("\n\n", context);

        String prompt = """
                You are a document question-answering assistant.

                Answer the user's question using ONLY the provided context.

                If the answer cannot be found in the context, say:
                "I could not find this information in the provided documents."

                Do not invent facts.
                Do not use outside knowledge.

                CONTEXT:
                %s

                QUESTION:
                %s

                ANSWER:
                """.formatted(
                contextText,
                question
        );

        Map<String, Object> request = Map.of(
                "model", generationModel,
                "prompt", prompt,
                "stream", false
        );

        OllamaGenerationResponse response =
                restClient.post()
                        .uri("/api/generate")
                        .body(request)
                        .retrieve()
                        .body(OllamaGenerationResponse.class);

        if (response == null ||
                response.response() == null ||
                response.response().isBlank()) {

            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return response.response().trim();
    }

    private record OllamaGenerationResponse(
            String response
    ) {}
}