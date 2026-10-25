package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Today's rates for the configured currencies. The last rate list is kept in
 * memory and reused until it is older than the cache TTL, which is never below
 * the 5 minutes the feed asks for.
 */
public class ExchangeRateService {

    static final Duration MIN_CACHE_TTL = Duration.ofMinutes(5);

    private static final String SOURCE = "Vietcombank";

    private final ExchangeRateClient client;

    private final List<String> currencies;

    private final Duration cacheTtl;

    private final Clock clock;

    private @Nullable FxQuote cached;

    ExchangeRateService(ExchangeRateClient client, List<String> currencies, Duration cacheTtl, Clock clock) {
        this.client = client;
        this.currencies = currencies.stream()
            .map((code) -> code.trim().toUpperCase(Locale.ROOT))
            .filter((code) -> !code.isEmpty())
            .distinct()
            .toList();
        this.cacheTtl = cacheTtl.compareTo(MIN_CACHE_TTL) < 0 ? MIN_CACHE_TTL : cacheTtl;
        this.clock = clock;
    }

    public ExchangeRates current() {
        FxQuote quote = quote();
        List<ExchangeRate> rates = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String code : this.currencies) {
            Optional<ExchangeRate> rate = quote.rows()
                .stream()
                .filter((row) -> row.currencyCode().equalsIgnoreCase(code))
                .findFirst()
                .flatMap(ExchangeRateService::rate);
            rate.ifPresentOrElse(rates::add, () -> missing.add(code));
        }
        return new ExchangeRates(List.copyOf(rates), List.copyOf(missing), quote.quotedAt(), quote.fetchedAt(),
                SOURCE);
    }

    private static Optional<ExchangeRate> rate(FxQuote.Row row) {
        BigDecimal cashBuy = row.cashBuy();
        BigDecimal transferBuy = row.transferBuy();
        if (cashBuy != null) {
            return Optional.of(new ExchangeRate(row.currencyCode(), row.currencyName(), cashBuy, transferBuy,
                    row.sell(), cashBuy, "CASH_BUY"));
        }
        if (transferBuy != null) {
            return Optional.of(new ExchangeRate(row.currencyCode(), row.currencyName(), null, transferBuy,
                    row.sell(), transferBuy, "TRANSFER_BUY"));
        }
        return Optional.empty();
    }

    private synchronized FxQuote quote() {
        FxQuote quote = this.cached;
        if (quote == null || quote.fetchedAt().plus(this.cacheTtl).isBefore(this.clock.instant())) {
            quote = this.client.fetch();
            this.cached = quote;
        }
        return quote;
    }

}
