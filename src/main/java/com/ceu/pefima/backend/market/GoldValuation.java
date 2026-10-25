package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * Market value of the configured gold holding at today's buy price. It is a live
 * figure, not tied to any export snapshot.
 *
 * @param marketValue {@code quantityChi × buyPerChi}, in VND
 * @param source price feed name
 */
public record GoldValuation(BigDecimal quantityChi, BigDecimal buyPerChi, @Nullable BigDecimal sellPerChi,
        BigDecimal marketValue, String productCode, String productName, @Nullable LocalDateTime quotedAt,
        Instant fetchedAt, String source) {
}
