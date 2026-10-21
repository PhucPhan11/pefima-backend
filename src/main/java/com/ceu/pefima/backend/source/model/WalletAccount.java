package com.ceu.pefima.backend.source.model;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * A wallet account. Currency and timezone stay {@code null} until the export
 * provides them.
 *
 * @param labelsJson wallet labels as the text stored by the source
 * @param activeRaw the source's active flag text, uninterpreted
 * @param etfCompany the title of a Fund wallet's "ETF Company" relation, never
 * its link
 */
public record WalletAccount(String accountId, @Nullable String name, @Nullable String labelsJson,
        @Nullable String activeRaw, @Nullable BigDecimal availableBalance, @Nullable BigDecimal startingBalance,
        @Nullable BigDecimal value, @Nullable String currencyCode, @Nullable String timezone,
        @Nullable String etfCompany, Provenance provenance) {
}
