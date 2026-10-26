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

class VietcombankRateClientTest {

    private static final URI RATE_URL = URI.create("https://rates.example.test/fx");

    private static final Instant NOW = Instant.parse("2026-01-04T03:00:00Z");

    private MockRestServiceServer server;

    private VietcombankRateClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = new VietcombankRateClient(builder.build(), RATE_URL, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void readsEveryRateInVndPerUnit() {
        respond("""
                <!--For reference only.-->
                <ExrateList>
                  <DateTime>1/4/2026 9:05:07 AM</DateTime>
                  <Exrate CurrencyCode="AAA" CurrencyName="SYNTHETIC ONE   " Buy="1,234.50" Transfer="1,240.00" Sell="1,300.25" />
                  <Exrate CurrencyCode="BBB" CurrencyName="SYNTHETIC TWO" Buy="-" Transfer="12.34" Sell="13.00" />
                  <Source>Synthetic bank</Source>
                </ExrateList>
                """);
        FxQuote quote = this.client.fetch();
        assertThat(quote.quotedAt()).isEqualTo(LocalDateTime.of(2026, 1, 4, 9, 5, 7));
        assertThat(quote.fetchedAt()).isEqualTo(NOW);
        assertThat(quote.rows()).hasSize(2);
        FxQuote.Row first = quote.rows().get(0);
        assertThat(first.currencyCode()).isEqualTo("AAA");
        assertThat(first.currencyName()).isEqualTo("SYNTHETIC ONE");
        assertThat(first.cashBuy()).isEqualByComparingTo(new BigDecimal("1234.50"));
        assertThat(first.transferBuy()).isEqualByComparingTo(new BigDecimal("1240.00"));
        assertThat(first.sell()).isEqualByComparingTo(new BigDecimal("1300.25"));
        FxQuote.Row second = quote.rows().get(1);
        assertThat(second.cashBuy()).isNull();
        assertThat(second.transferBuy()).isEqualByComparingTo(new BigDecimal("12.34"));
        this.server.verify();
    }

    @Test
    void leavesAnOddDateTimeOut() {
        respond("""
                <ExrateList><DateTime>yesterday</DateTime>
                <Exrate CurrencyCode="AAA" CurrencyName="SYNTHETIC" Buy="10" Transfer="11" Sell="12" /></ExrateList>
                """);
        assertThat(this.client.fetch().quotedAt()).isNull();
    }

    @Test
    void failsOnMalformedXml() {
        respond("<ExrateList><Exrate CurrencyCode=\"AAA\"");
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.client::fetch)
            .withMessageContaining("not valid XML");
    }

    @Test
    void rejectsADoctype() {
        respond("""
                <?xml version="1.0"?>
                <!DOCTYPE ExrateList [<!ENTITY name "SYNTHETIC">]>
                <ExrateList><Exrate CurrencyCode="AAA" CurrencyName="&name;" Buy="10" Transfer="11" Sell="12" /></ExrateList>
                """);
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.client::fetch)
            .withMessageContaining("not valid XML");
    }

    @Test
    void failsWhenNoRateIsListed() {
        respond("<ExrateList><DateTime>1/4/2026 9:05:07 AM</DateTime></ExrateList>");
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.client::fetch)
            .withMessageContaining("no rates");
    }

    @Test
    void failsOnAServerError() {
        this.server.expect(requestTo(RATE_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.client::fetch)
            .withMessageContaining("Could not read");
    }

    private void respond(String xml) {
        this.server.expect(requestTo(RATE_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(xml, MediaType.TEXT_XML));
    }

}
