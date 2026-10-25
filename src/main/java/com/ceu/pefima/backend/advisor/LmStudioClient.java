package com.ceu.pefima.backend.advisor;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ceu.pefima.backend.advisor.ProfileAnalysis.AiReview;

/**
 * Asks LM Studio's OpenAI-compatible chat endpoint for a review in the
 * {@link AdvisorPrompt#responseFormat()} schema.
 */
class LmStudioClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;

    private final URI baseUrl;

    private final String model;

    private final double temperature;

    private final int maxTokens;

    LmStudioClient(RestClient restClient, URI baseUrl, String model, double temperature, int maxTokens) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    String model() {
        return this.model;
    }

    AiReview review(String system, String user) {
        // reasoning_effort "none" turns off the thinking pass of reasoning models such as
        // gemma 4, which otherwise spends most of the token budget before answering.
        Map<String, Object> request = Map.of("model", this.model, "temperature", this.temperature, "stream", false,
                "max_tokens", this.maxTokens, "reasoning_effort", "none", "messages",
                List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)),
                "response_format", AdvisorPrompt.responseFormat());
        JsonNode response;
        try {
            response = this.restClient.post()
                .uri(this.baseUrl.resolve("/v1/chat/completions"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        }
        catch (RestClientException ex) {
            if (timedOut(ex)) {
                throw new AdvisorUnavailableException("LM Studio did not answer in time. The model may be too slow on "
                        + "this machine; try again, or raise pefima.advisor.timeout.", ex);
            }
            throw new AdvisorUnavailableException("Could not reach LM Studio at " + this.baseUrl.getAuthority()
                    + ". Open LM Studio, run `lms server start` and load " + this.model + ".", ex);
        }
        if (response == null) {
            throw new AdvisorUnavailableException("LM Studio returned an empty answer");
        }
        String content = response.path("choices").path(0).path("message").path("content").asString("");
        return parse(stripFences(content));
    }

    private AiReview parse(String content) {
        JsonNode root;
        try {
            root = JSON.readTree(content);
        }
        catch (JacksonException ex) {
            throw new AdvisorUnavailableException("LM Studio did not answer in the expected JSON format", ex);
        }
        String summary = root.path("profileSummary").asString("").trim();
        String riskReading = root.path("riskReading").asString("").trim();
        if (summary.isEmpty() || riskReading.isEmpty()) {
            throw new AdvisorUnavailableException("LM Studio's answer is missing the profile review");
        }
        Map<RiskScenario, String> comments = new EnumMap<>(RiskScenario.class);
        for (RiskScenario scenario : RiskScenario.values()) {
            String comment = root.path("scenarioComments").path(scenario.name()).asString("").trim();
            if (!comment.isEmpty()) {
                comments.put(scenario, comment);
            }
        }
        List<String> warnings = new ArrayList<>();
        for (JsonNode warning : root.path("warnings")) {
            String text = warning.asString("").trim();
            if (!text.isEmpty()) {
                warnings.add(text);
            }
        }
        return new AiReview(summary, riskReading, Map.copyOf(comments), List.copyOf(warnings), this.model);
    }

    private static boolean timedOut(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** Some models wrap JSON in a Markdown code fence even with a schema. */
    private static String stripFences(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            int end = text.lastIndexOf("```");
            if (start >= 0 && end > start) {
                return text.substring(start + 1, end).trim();
            }
        }
        return text;
    }

}
