package com.ceu.pefima.backend.advisor;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ceu.pefima.backend.advisor.ProfileAnalysis.AiReview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LmStudioClientTest {

    private static final String ENDPOINT = "http://127.0.0.1:1234/v1/chat/completions";

    private MockRestServiceServer server;

    private LmStudioClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = new LmStudioClient(builder.build(), URI.create("http://127.0.0.1:1234"), "synthetic-model",
                0.3, 500);
    }

    @Test
    void sendsTheSchemaAndReadsTheReview() {
        this.server.expect(requestTo(ENDPOINT))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.model").value("synthetic-model"))
            .andExpect(jsonPath("$.response_format.type").value("json_schema"))
            .andExpect(jsonPath("$.reasoning_effort").value("none"))
            .andExpect(jsonPath("$.max_tokens").value(500))
            .andExpect(jsonPath("$.messages[0].role").value("system"))
            .andRespond(withSuccess(answer("""
                    {"profileSummary":"Synthetic summary","riskReading":"Synthetic reading",
                     "scenarioComments":{"CONSERVATIVE":"c","BALANCED":"b","GROWTH":"g","AGGRESSIVE":"a"},
                     "warnings":["w1"," "]}
                    """), MediaType.APPLICATION_JSON));
        AiReview review = this.client.review("system", "user");
        assertThat(review.profileSummary()).isEqualTo("Synthetic summary");
        assertThat(review.riskReading()).isEqualTo("Synthetic reading");
        assertThat(review.scenarioComments()).containsEntry(RiskScenario.GROWTH, "g").hasSize(4);
        assertThat(review.warnings()).containsExactly("w1");
        assertThat(review.model()).isEqualTo("synthetic-model");
        this.server.verify();
    }

    @Test
    void acceptsJsonInACodeFence() {
        this.server.expect(requestTo(ENDPOINT))
            .andRespond(withSuccess(answer("""
                    ```json
                    {"profileSummary":"s","riskReading":"r","scenarioComments":{},"warnings":[]}
                    ```"""), MediaType.APPLICATION_JSON));
        AiReview review = this.client.review("system", "user");
        assertThat(review.profileSummary()).isEqualTo("s");
        assertThat(review.scenarioComments()).isEmpty();
    }

    @Test
    void failsOnAnAnswerThatIsNotJson() {
        this.server.expect(requestTo(ENDPOINT))
            .andRespond(withSuccess(answer("not json at all"), MediaType.APPLICATION_JSON));
        assertThatExceptionOfType(AdvisorUnavailableException.class)
            .isThrownBy(() -> this.client.review("system", "user"))
            .withMessageContaining("JSON");
    }

    @Test
    void failsWhenTheReviewIsMissing() {
        this.server.expect(requestTo(ENDPOINT))
            .andRespond(withSuccess(answer("{\"riskReading\":\"r\"}"), MediaType.APPLICATION_JSON));
        assertThatExceptionOfType(AdvisorUnavailableException.class)
            .isThrownBy(() -> this.client.review("system", "user"))
            .withMessageContaining("missing");
    }

    @Test
    void failsOnAServerError() {
        this.server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatExceptionOfType(AdvisorUnavailableException.class)
            .isThrownBy(() -> this.client.review("system", "user"))
            .withMessageContaining("lms server start");
    }

    /** An OpenAI-style chat completion whose message content is the given text. */
    private static String answer(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}]}";
    }

}
