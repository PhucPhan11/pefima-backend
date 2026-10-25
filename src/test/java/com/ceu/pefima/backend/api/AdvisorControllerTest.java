package com.ceu.pefima.backend.api;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.ceu.pefima.backend.advisor.AdvisorService;
import com.ceu.pefima.backend.advisor.AssetClass;
import com.ceu.pefima.backend.advisor.InvalidProfileException;
import com.ceu.pefima.backend.advisor.ProfileAnalysis;
import com.ceu.pefima.backend.advisor.ProfileAnalysis.AllocationLine;
import com.ceu.pefima.backend.advisor.RiskScenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@WebMvcTest(AdvisorController.class)
class AdvisorControllerTest {

    private static final String BODY = """
            {"holdings":[{"assetClass":"SAFE","value":70},{"assetClass":"ETF","value":30}],
             "debts":0,"monthlyExpensesAvg":10,"monthsWithData":3}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AdvisorService advisor;

    @Test
    void returnsTheAnalysis() {
        given(this.advisor.analyse(any(), eq(true))).willReturn(new ProfileAnalysis(new BigDecimal("100"),
                BigDecimal.ZERO,
                List.of(new AllocationLine(AssetClass.SAFE, "Tiền mặt & tiết kiệm", new BigDecimal("70"),
                        new BigDecimal("70"))),
                null, List.of(), RiskScenario.BALANCED, null, "synthetic error", "disclaimer"));
        assertThat(this.mvc.post()
            .uri("/api/advisor/analysis?ai=true")
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY)).hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                    {"totalAssets":100,"closest":"BALANCED","ai":null,"aiError":"synthetic error",
                     "current":[{"assetClass":"SAFE","percent":70}]}
                    """);
    }

    @Test
    void answers400ForAnInvalidProfile() {
        given(this.advisor.analyse(any(), eq(false)))
            .willThrow(new InvalidProfileException("The profile has no positive holdings"));
        assertThat(this.mvc.post().uri("/api/advisor/analysis").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.title")
            .isEqualTo("Invalid profile");
    }

}
