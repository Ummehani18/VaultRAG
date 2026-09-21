package com.vaultRag.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final RestClient restClient;
    private final String embeddingModel;

    public EmbeddingService(
            @Value("${vaultrag.ollama.base-url}")
            String baseUrl,
            @Value("${vaultrag.ollama.embedding-model}")
            String embeddingModel) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.embeddingModel = embeddingModel;
    }

    public List<Double> generateEmbedding(String text) {

        Map<String, Object> request = Map.of(
                "model", embeddingModel,
                "input", text
        );

        OllamaEmbeddingResponse response =
                restClient.post()
                        .uri("/api/embed")
                        .body(request)
                        .retrieve()
                        .body(OllamaEmbeddingResponse.class);

        if (response == null ||
                response.embeddings() == null ||
                response.embeddings().isEmpty()) {

            throw new IllegalStateException(
                    "Ollama returned an empty embedding"
            );
        }

        return response.embeddings().get(0);
    }

    private record OllamaEmbeddingResponse(
            List<List<Double>> embeddings
    ) {}
}