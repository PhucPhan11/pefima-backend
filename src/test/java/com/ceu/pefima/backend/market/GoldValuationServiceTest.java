package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GoldValuationServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-04T03:00:00Z"));

    private final CountingClient client = new CountingClient(this.clock);

    private final GoldValuationService service = new GoldValuationService(this.client, new BigDecimal("3"),
            Duration.ofMinutes(15), this.clock);

    @Test
    void valuesTheQuantityAtTheBuyPrice() {
        GoldValuation valuation = this.service.current();
        assertThat(valuation.quantityChi()).isEqualByComparingTo("3");
        assertThat(valuation.buyPerChi()).isEqualByComparingTo("1000000");
        assertThat(valuation.sellPerChi()).isEqualByComparingTo("1100000");
        assertThat(valuation.marketValue()).isEqualByComparingTo("3000000");
        assertThat(valuation.productCode()).isEqualTo("N24K");
        assertThat(valuation.source()).isEqualTo("PNJ");
    }

    @Test
    void reusesTheQuoteWithinTheTtlAndRefreshesAfterIt() {
        this.service.current();
        this.clock.advance(Duration.ofMinutes(15));
        this.service.current();
        assertThat(this.client.calls).isEqualTo(1);
        this.clock.advance(Duration.ofSeconds(1));
        this.service.current();
        assertThat(this.client.calls).isEqualTo(2);
    }

    @Test
    void retriesAfterAFailedFetch() {
        this.client.failNext = true;
        assertThatExceptionOfType(MarketPriceUnavailableException.class).isThrownBy(this.service::current);
        assertThat(this.service.current().marketValue()).isEqualByComparingTo("3000000");
        assertThat(this.client.calls).isEqualTo(2);
    }

    private static final class CountingClient implements GoldPriceClient {

        private final Clock clock;

        int calls;

        boolean failNext;

        CountingClient(Clock clock) {
            this.clock = clock;
        }

        @Override
        public GoldQuote fetch() {
            this.calls++;
            if (this.failNext) {
                this.failNext = false;
                throw new MarketPriceUnavailableException("synthetic failure");
            }
            return new GoldQuote("N24K", "Ring gold 999.9", new BigDecimal("1000000"), new BigDecimal("1100000"),
                    null, this.clock.instant());
        }

    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }

    }

}
