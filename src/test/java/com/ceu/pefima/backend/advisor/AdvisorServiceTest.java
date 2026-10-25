package com.ceu.pefima.backend.advisor;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

import com.ceu.pefima.backend.advisor.ProfileAnalysis.AiReview;
import com.ceu.pefima.backend.advisor.ProfileInput.Holding;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisorServiceTest {

    private static final ProfileInput INPUT = new ProfileInput(
            List.of(new Holding(AssetClass.SAFE, new BigDecimal("70")), new Holding(AssetClass.ETF, new BigDecimal("30"))),
            null, null, null, new BigDecimal("10"), null, 3);

    @Test
    void skipsTheLlmWhenNotAsked() {
        FakeClient client = new FakeClient(null);
        ProfileAnalysis analysis = new AdvisorService(client).analyse(INPUT, false);
        assertThat(client.calls).isZero();
        assertThat(analysis.ai()).isNull();
        assertThat(analysis.aiError()).isNull();
        assertThat(analysis.scenarios()).hasSize(4);
    }

    @Test
    void addsTheReviewAndPassesTheComputedFiguresToTheLlm() {
        AiReview review = new AiReview("s", "r", Map.of(RiskScenario.BALANCED, "b"), List.of(), "synthetic-model");
        FakeClient client = new FakeClient(review);
        ProfileAnalysis analysis = new AdvisorService(client).analyse(INPUT, true);
        assertThat(analysis.ai()).isEqualTo(review);
        assertThat(analysis.aiError()).isNull();
        assertThat(client.calls).isEqualTo(1);
        assertThat(client.user).contains("Thận trọng", "Mạo hiểm", "Kịch bản gần hồ sơ hiện tại nhất", "tăng ",
                "giảm ");
        assertThat(client.system).contains("không phải tư vấn đầu tư");
    }

    @Test
    void keepsTheComparisonWhenTheLlmFails() {
        FakeClient client = new FakeClient(null);
        ProfileAnalysis analysis = new AdvisorService(client).analyse(INPUT, true);
        assertThat(analysis.ai()).isNull();
        assertThat(analysis.aiError()).contains("lms server start");
        assertThat(analysis.scenarios()).hasSize(4);
        assertThat(analysis.totalAssets()).isEqualByComparingTo("100");
    }

    private static final class FakeClient extends LmStudioClient {

        private final AiReview review;

        int calls;

        String system = "";

        String user = "";

        FakeClient(AiReview review) {
            super(RestClient.create(), URI.create("http://127.0.0.1:1"), "synthetic-model", 0.3, 500);
            this.review = review;
        }

        @Override
        AiReview review(String system, String user) {
            this.calls++;
            this.system = system;
            this.user = user;
            if (this.review == null) {
                throw new AdvisorUnavailableException("Could not reach LM Studio. Run `lms server start`.");
            }
            return this.review;
        }

    }

}
