package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * Today's gold price, and the configured holding's value when a quantity is set.
 * A live figure, not tied to any export snapshot.
 *
 * @param quantityChi the configured quantity, or {@code null}
 * @param marketValue {@code quantityChi × buyPerChi} in VND, or {@code null} without a quantity
 * @param source price feed name
 */
public record GoldValuation(@Nullable BigDecimal quantityChi, BigDecimal buyPerChi, @Nullable BigDecimal sellPerChi,
        @Nullable BigDecimal marketValue, String productCode, String productName, @Nullable LocalDateTime quotedAt,
        Instant fetchedAt, String source) {
}
