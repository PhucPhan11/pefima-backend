package com.ceu.pefima.backend.api;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(MarketController.class)
class MarketControllerWithoutGoldTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void answers503WhenGoldIsNotEnabled() {
        assertThat(this.mvc.get().uri("/api/market/gold")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
            .bodyJson()
            .extractingPath("$.detail")
            .asString()
            .contains("pefima.gold.enabled");
    }

    @Test
    void answers503WhenNoCurrencyIsConfigured() {
        assertThat(this.mvc.get().uri("/api/market/fx-rates")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
            .bodyJson()
            .extractingPath("$.detail")
            .asString()
            .contains("pefima.fx.currencies");
    }

}
