package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A full exchange-rate list as the feed published it, in VND per unit.
 *
 * @param quotedAt the feed's own update time, in Vietnam local time
 * @param fetchedAt when the backend fetched the list
 */
public record FxQuote(List<Row> rows, @Nullable LocalDateTime quotedAt, Instant fetchedAt) {

    /**
     * One currency. A rate the bank does not quote is {@code null}.
     *
     * @param cashBuy price the bank pays for cash
     * @param transferBuy price the bank pays for a transfer
     * @param sell price the bank charges
     */
    public record Row(String currencyCode, String currencyName, @Nullable BigDecimal cashBuy,
            @Nullable BigDecimal transferBuy, @Nullable BigDecimal sell) {
    }

}
