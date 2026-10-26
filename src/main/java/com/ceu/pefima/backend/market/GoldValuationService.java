package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * Today's gold buy price, and the configured holding's value when a quantity is
 * set (the holding normally comes from the Gold wallet in chỉ). The last quote
 * is kept in memory and reused until it is older than the cache TTL, so opening
 * a page refreshes the price at most once per TTL.
 */
public class GoldValuationService {

    private static final String SOURCE = "PNJ";

    private final GoldPriceClient client;

    private final @Nullable BigDecimal quantityChi;

    private final Duration cacheTtl;

    private final Clock clock;

    private @Nullable GoldQuote cached;

    GoldValuationService(GoldPriceClient client, @Nullable BigDecimal quantityChi, Duration cacheTtl, Clock clock) {
        this.client = client;
        this.quantityChi = quantityChi;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    public GoldValuation current() {
        GoldQuote quote = quote();
        BigDecimal quantity = this.quantityChi;
        return new GoldValuation(quantity, quote.buyPerChi(), quote.sellPerChi(),
                quantity == null ? null : quantity.multiply(quote.buyPerChi()), quote.productCode(), quote.productName(),
                quote.quotedAt(), quote.fetchedAt(), SOURCE);
    }

    private synchronized GoldQuote quote() {
        GoldQuote quote = this.cached;
        if (quote == null || quote.fetchedAt().plus(this.cacheTtl).isBefore(this.clock.instant())) {
            quote = this.client.fetch();
            this.cached = quote;
        }
        return quote;
    }

}
