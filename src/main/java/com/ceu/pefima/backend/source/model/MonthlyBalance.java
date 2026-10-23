package com.ceu.pefima.backend.source.model;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * One row of the Notion Balance database: a month's cash flow, or the
 * {@code Year} row summing the months. Values are exported formula values, not
 * recomputed.
 *
 * @param period the source label, {@code January} to {@code December} or {@code Year}
 * @param month 1 to 12, or {@code null} for the {@code Year} row
 * @param net income minus expenses (Notion's {@code Monthly End Balance}); not an
 * account balance
 * @param yearEnd the year's summed net, populated on the {@code Year} row
 */
public record MonthlyBalance(String balanceId, @Nullable String period, @Nullable Integer month,
        @Nullable BigDecimal income, @Nullable BigDecimal expenses, @Nullable BigDecimal net,
        @Nullable BigDecimal yearEnd, Provenance provenance) {
}
