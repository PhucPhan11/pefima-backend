package com.ceu.pefima.backend.market;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Today's rates for the configured currencies. A live figure, not tied to any
 * export snapshot.
 *
 * @param rates configured currencies that have a buy rate, in configured order
 * @param missing configured currencies the feed does not list or does not buy
 * @param source rate feed name
 */
public record ExchangeRates(List<ExchangeRate> rates, List<String> missing, @Nullable LocalDateTime quotedAt,
        Instant fetchedAt, String source) {
}
