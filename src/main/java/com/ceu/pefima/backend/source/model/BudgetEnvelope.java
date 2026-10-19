package com.ceu.pefima.backend.source.model;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * A budget envelope such as the Emergency Fund. Balances are exported formula
 * values, not recomputed.
 */
public record BudgetEnvelope(String budgetId, @Nullable String name, @Nullable BigDecimal availableBalance,
        @Nullable BigDecimal startingBalance, @Nullable String emergencyRelationId, Provenance provenance) {
}
