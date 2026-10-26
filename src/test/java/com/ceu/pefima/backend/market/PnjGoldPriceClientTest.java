package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PnjGoldPriceClientTest {

    private static final URI PRICE_URL = URI.create("https://prices.example.test/gold");

    private static final Instant NOW = Instant.parse("2026-01-04T03:00:00Z");

    private MockRestServiceServer server;

    private PnjGoldPriceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = new PnjGoldPriceClient(builder.build(), PRICE_URL, "N24K", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void readsTheConfiguredProductInVndPerChi() {
        respond("""
                {"data":[
                  {"masp":"SJC","tensp":"Bar gold","giaban":2200,"giamua":2100},
                  {"masp":"N24K","tensp":"Ring gold 999.9","giaban":1250,"giamua":1200},
                  {"masp":"RAW","tensp":"Raw gold","giaban":"","giamua":1000}
                ],"updateDate":"04/01/2026 09:30:15"}
                """);
        GoldQuote quote = this.client.fetch();
        assertThat(quote.productCode()).isEqualTo("N24K");
        assertThat(quote.productName()).isEqualTo("Ring gold 999.9");
        assertThat(quote.buyPerChi()).isEqualByComparingTo(new BigDecimal("1200000"));
        assertThat(quote.sellPerChi()).isEqualByComparingTo(new BigDecimal("1250000"));
        assertThat(quote.quotedAt()).isEqualTo(LocalDateTime.of(2026, 1, 4, 9, 30, 15));
        assertThat(quote.fetchedAt()).isEqualTo(NOW);
        this.server.verify();
    }

    @Test
    void treatsAnEmptySellPriceAndAnOddDateAsMissing() {
        respond("""
                {"data":[{"masp":"N24K","tensp":"Ring gold 999.9","giaban":"","giamua":"1,200"}],
                 "updateDate":"yesterday"}
                """);
        GoldQuote quote = this.client.fetch();
        assertThat(quote.buyPerChi()).isEqualByComparingTo(new BigDecimal("1200000"));
        assertThat(quote.sellPerChi()).isNull();
        assertThat(quote.quotedAt()).isNull();
    }

    @Test
    void failsWhenTheProductIsNotListed() {
        respond("""
                {"data":[{"masp":"SJC","tensp":"Bar gold","giaban":2200,"giamua":2100}]}
                """);
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.client::fetch)
            .withMessageContaining("N24K");
    }

    @Test
    void failsWhenTheProductHasNoBuyPrice() {
        respond("""
                {"data":[{"masp":"N24K","tensp":"Ring gold 999.9","giaban":1250,"giamua":""}]}
                """);
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.client::fetch)
            .withMessageContaining("no buy price");
    }

    @Test
    void failsOnAServerError() {
        this.server.expect(requestTo(PRICE_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.client::fetch)
            .withMessageContaining("Could not read");
    }

    private void respond(String json) {
        this.server.expect(requestTo(PRICE_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

}
