
package com.vaultRag.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class LlmService {

    private static final Logger log =
            LoggerFactory.getLogger(LlmService.class);

    private final RestClient restClient;
    private final String generationModel;
    private final JsonMapper objectMapper;
    private final Map<String, Object> generationOptions;

    public LlmService(
            @Value("${vaultrag.ollama.base-url}")
            String baseUrl,

            @Value("${vaultrag.ollama.generation-model}")
            String generationModel,

            @Value("${vaultrag.ollama.temperature:0.2}")
            double temperature,

            @Value("${vaultrag.ollama.seed:42}")
            int seed,

            @Value("${vaultrag.ollama.num-predict:300}")
            int numPredict,

            @Value("${vaultrag.ollama.repeat-penalty:1.1}")
            double repeatPenalty,

            @Value("${vaultrag.ollama.top-p:0.8}")
            double topP,

            @Value("${vaultrag.ollama.top-k:40}")
            int topK) {

        if (numPredict <= 0) {
            throw new IllegalArgumentException(
                    "num-predict must be greater than zero"
            );
        }

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.generationModel = generationModel;
        this.objectMapper = JsonMapper.builder().build();

        this.generationOptions = Map.of(
                "temperature", temperature,
                "seed", seed,
                "num_predict", numPredict,
                "repeat_penalty", repeatPenalty,
                "top_p", topP,
                "top_k", topK
        );

        log.info(
                "[LLM] Initialized: model={}, options={}",
                generationModel,
                generationOptions
        );
    }

    /**
     * Builds the same prompt for streaming and non-streaming requests.
     */
    private String buildPrompt(
            String question,
            List<String> context) {

        String contextText = String.join(
                "\n\n",
                context.stream()
                        .filter(item -> item != null && !item.isBlank())
                        .toList()
        );

        return """
                You are VaultRAG, a document-grounded question-answering assistant.

                Your task is to answer the QUESTION using only information
                explicitly supported by the DOCUMENT CONTEXT.

                STRICT RULES:

                1. Identify exactly what the question asks.
                2. Answer the question directly.
                3. Use only facts supported by the supplied context.
                4. Distinguish explicit recommendations from technologies
                   that are merely mentioned or used as examples.
                5. Never introduce technologies, alternatives, or
                   recommendations from your general knowledge.
                6. Do not confuse frontend, backend, database, and
                   infrastructure technologies.
                7. Preserve technology names as written in the context.
                8. Do not infer that something is recommended merely
                   because it appears in the document.
                9. If the context does not support an answer, respond:
                   I could not find this information in the provided documents.
                10. Keep the answer concise.
                11. Do not repeat sentences, phrases, or list items.
                12. Do not list the same technology multiple times.
                13. Group related information instead of repeating details.
                14. Do not produce an exhaustive list unless requested.
                15. Stop after answering the question.
                16. Never continue generating content just to fill the response.

                OUTPUT REQUIREMENTS:

                - Answer the exact question asked.
                - Include only relevant, supported information.
                - Use a short paragraph or concise bullet list.
                - For simple questions, prefer 1-5 bullet points.
                - Do not include unrelated implementation details.
                - Do not repeat information already stated.
                - Do not add generic introductions or conclusions.

                DOCUMENT CONTEXT:
                <context>
                %s
                </context>

                QUESTION:
                %s

                ANSWER:
                """.formatted(
                contextText,
                question
        );
    }

    /**
     * Generates a complete non-streaming answer.
     */
    public String generateAnswer(
            String question,
            List<String> context) {

        validateInput(question, context);

        String prompt = buildPrompt(question, context);

        Map<String, Object> request = Map.of(
                "model", generationModel,
                "prompt", prompt,
                "stream", false,
                "options", generationOptions
        );

        OllamaGenerationResponse response =
                restClient.post()
                        .uri("/api/generate")
                        .body(request)
                        .retrieve()
                        .body(OllamaGenerationResponse.class);

        if (response == null
                || response.response() == null
                || response.response().isBlank()) {

            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        log.info(
                "[LLM] Generation completed: model={}, done={}, reason={}, promptTokens={}, generatedTokens={}",
                generationModel,
                response.done(),
                response.done_reason(),
                response.prompt_eval_count(),
                response.eval_count()
        );

        return response.response().trim();
    }

    /**
     * Generates an answer as a stream of text chunks.
     */
    public void streamAnswer(
            String question,
            List<String> context,
            Consumer<String> onChunk) {

        validateInput(question, context);

        if (onChunk == null) {
            throw new IllegalArgumentException(
                    "Chunk consumer must not be null"
            );
        }

        String prompt = buildPrompt(question, context);

        Map<String, Object> request = Map.of(
                "model", generationModel,
                "prompt", prompt,
                "stream", true,
                "options", generationOptions
        );

        restClient.post()
                .uri("/api/generate")
                .body(request)
                .exchange((requestObject, response) -> {

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "Ollama streaming request failed with status: "
                                        + response.getStatusCode()
                        );
                    }

                    boolean generationCompleted = false;
                    long streamedCharacters = 0;

                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 response.getBody(),
                                                 StandardCharsets.UTF_8
                                         ))) {

                        String line;

                        while ((line = reader.readLine()) != null) {

                            if (line.isBlank()) {
                                continue;
                            }

                            JsonNode json =
                                    objectMapper.readTree(line);

                            JsonNode responseNode =
                                    json.get("response");

                            if (responseNode != null
                                    && !responseNode.isNull()) {

                                String chunk = responseNode.asText();

                                if (!chunk.isEmpty()) {
                                    streamedCharacters += chunk.length();
                                    onChunk.accept(chunk);
                                }
                            }

                            JsonNode doneNode = json.get("done");

                            if (doneNode != null
                                    && doneNode.asBoolean()) {

                                generationCompleted = true;

                                JsonNode reasonNode =
                                        json.get("done_reason");

                                log.info(
                                        "[LLM] Streaming completed: model={}, reason={}, characters={}",
                                        generationModel,
                                        reasonNode != null
                                                ? reasonNode.asText()
                                                : "unknown",
                                        streamedCharacters
                                );

                                break;
                            }
                        }

                    } catch (IOException e) {

                        throw new IllegalStateException(
                                "Failed to read Ollama streaming response",
                                e
                        );
                    }

                    if (!generationCompleted) {
                        log.warn(
                                "[LLM] Stream ended without receiving a done event. Characters received={}",
                                streamedCharacters
                        );
                    }

                    return null;
                });
    }

    /**
     * Validates incoming generation input.
     */
    private void validateInput(
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

        boolean hasValidContext = context.stream()
                .anyMatch(item -> item != null && !item.isBlank());

        if (!hasValidContext) {
            throw new IllegalArgumentException(
                    "Context must contain at least one non-empty passage"
            );
        }
    }

    /**
     * Ollama non-streaming response.
     */
    private record OllamaGenerationResponse(
            String response,
            Boolean done,
            String done_reason,
            Long prompt_eval_count,
            Long eval_count,
            Long total_duration
    ) {
    }
}
