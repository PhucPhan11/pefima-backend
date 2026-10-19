package com.ceu.pefima.backend.source.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * An expense, income, or internal transfer. Fields ending in {@code Raw} or
 * {@code Json} hold source text that has not been interpreted yet.
 *
 * @param occurredOn date part of the transaction date
 * @param occurredAt full timestamp, or {@code null} when the source only has a date
 * @param rawDateText the date exactly as exported
 * @param budgetId the resolved budget, or {@code null} when both a default and an
 * inserted allocation exist (ambiguous; see {@code defaultBudgetId} and {@code insertBudgetId})
 */
public record Transaction(String transactionId, TransactionKind kind, @Nullable String name,
        @Nullable BigDecimal amount, @Nullable LocalDate occurredOn, @Nullable LocalDateTime occurredAt,
        @Nullable String rawDateText, @Nullable String month, @Nullable String fromAccountId,
        @Nullable String toAccountId, @Nullable String budgetId, @Nullable String defaultBudgetId,
        @Nullable String insertBudgetId, @Nullable String tagsJson, @Nullable String description,
        @Nullable String notes, @Nullable String conditionRaw, @Nullable String paidRaw, @Nullable String spentRaw,
        @Nullable BigDecimal differ, @Nullable BigDecimal monitor, @Nullable String creditHoldRaw,
        @Nullable String overpayRaw, Provenance provenance) {
}
