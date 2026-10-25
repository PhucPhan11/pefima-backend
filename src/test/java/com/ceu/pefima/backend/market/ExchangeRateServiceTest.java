package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-04T03:00:00Z"));

    private final CountingClient client = new CountingClient(this.clock);

    @Test
    void returnsConfiguredCurrenciesAtTheCashBuyRateElseTheTransferBuyRate() {
        ExchangeRates rates = service(List.of(" bbb", "AAA", "ZZZ", "NNN", "AAA"), Duration.ofMinutes(15)).current();
        assertThat(rates.rates()).extracting(ExchangeRate::currencyCode).containsExactly("BBB", "AAA");
        ExchangeRate transfer = rates.rates().get(0);
        assertThat(transfer.rate()).isEqualByComparingTo("12.34");
        assertThat(transfer.rateType()).isEqualTo("TRANSFER_BUY");
        assertThat(transfer.cashBuy()).isNull();
        ExchangeRate cash = rates.rates().get(1);
        assertThat(cash.rate()).isEqualByComparingTo("1234.5");
        assertThat(cash.rateType()).isEqualTo("CASH_BUY");
        assertThat(cash.transferBuy()).isEqualByComparingTo("1240");
        assertThat(rates.missing()).containsExactly("ZZZ", "NNN");
        assertThat(rates.source()).isEqualTo("Vietcombank");
    }

    @Test
    void reusesTheListWithinTheTtlAndRefreshesAfterIt() {
        ExchangeRateService service = service(List.of("AAA"), Duration.ofMinutes(15));
        service.current();
        this.clock.advance(Duration.ofMinutes(15));
        service.current();
        assertThat(this.client.calls).isEqualTo(1);
        this.clock.advance(Duration.ofSeconds(1));
        service.current();
        assertThat(this.client.calls).isEqualTo(2);
    }

    @Test
    void neverAsksMoreOftenThanEveryFiveMinutes() {
        ExchangeRateService service = service(List.of("AAA"), Duration.ofSeconds(30));
        service.current();
        this.clock.advance(Duration.ofMinutes(4));
        service.current();
        assertThat(this.client.calls).isEqualTo(1);
        this.clock.advance(Duration.ofMinutes(1).plusSeconds(1));
        service.current();
        assertThat(this.client.calls).isEqualTo(2);
    }

    private ExchangeRateService service(List<String> currencies, Duration ttl) {
        return new ExchangeRateService(this.client, currencies, ttl, this.clock);
    }

    private static final class CountingClient implements ExchangeRateClient {

        private final Clock clock;

        int calls;

        CountingClient(Clock clock) {
            this.clock = clock;
        }

        @Override
        public FxQuote fetch() {
            this.calls++;
            return new FxQuote(List.of(
                    new FxQuote.Row("AAA", "SYNTHETIC ONE", new BigDecimal("1234.5"), new BigDecimal("1240"),
                            new BigDecimal("1300")),
                    new FxQuote.Row("BBB", "SYNTHETIC TWO", null, new BigDecimal("12.34"), new BigDecimal("13")),
                    new FxQuote.Row("NNN", "NOT BOUGHT", null, null, new BigDecimal("5"))), null, this.clock.instant());
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
