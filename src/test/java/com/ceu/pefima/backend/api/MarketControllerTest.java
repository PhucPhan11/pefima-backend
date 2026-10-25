package com.ceu.pefima.backend.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.ceu.pefima.backend.market.GoldValuation;
import com.ceu.pefima.backend.market.GoldValuationService;
import com.ceu.pefima.backend.market.MarketPriceUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(MarketController.class)
class MarketControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private GoldValuationService gold;

    @Test
    void returnsTheGoldValuation() {
        given(this.gold.current()).willReturn(new GoldValuation(new BigDecimal("2"), new BigDecimal("1000000"), null,
                new BigDecimal("2000000"), "N24K", "Ring gold 999.9", LocalDateTime.of(2026, 1, 4, 9, 30),
                Instant.parse("2026-01-04T03:00:00Z"), "PNJ"));
        assertThat(this.mvc.get().uri("/api/market/gold")).hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                    {"quantityChi":2,"buyPerChi":1000000,"sellPerChi":null,"marketValue":2000000,
                     "productCode":"N24K","productName":"Ring gold 999.9","quotedAt":"2026-01-04T09:30:00",
                     "fetchedAt":"2026-01-04T03:00:00Z","source":"PNJ"}
                    """);
    }

    @Test
    void answers502WhenThePriceIsUnavailable() {
        given(this.gold.current()).willThrow(new MarketPriceUnavailableException("PNJ lists no gold product N24K"));
        assertThat(this.mvc.get().uri("/api/market/gold")).hasStatus(HttpStatus.BAD_GATEWAY)
            .bodyJson()
            .isLenientlyEqualTo("""
                    {"title":"Market price unavailable","detail":"PNJ lists no gold product N24K"}
                    """);
    }

}
