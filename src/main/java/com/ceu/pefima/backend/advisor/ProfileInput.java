package com.ceu.pefima.backend.advisor;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * An aggregated investment profile, built by the frontend from one snapshot. It
 * carries totals per asset class only, never wallet names or IDs.
 *
 * @param holdings VND value per asset class, each at least 0
 * @param debts total of the negative wallets, as a positive number
 * @param emergencyFundBalance the Emergency Fund budget's balance
 * @param emergencyWalletsTotal VND held in the wallets counted for the Emergency Fund
 * @param monthlyExpensesAvg average monthly expenses over {@code monthsWithData}
 * @param monthlyIncomeAvg average monthly income over {@code monthsWithData}
 */
public record ProfileInput(List<Holding> holdings, @Nullable BigDecimal debts,
        @Nullable BigDecimal emergencyFundBalance, @Nullable BigDecimal emergencyWalletsTotal,
        @Nullable BigDecimal monthlyExpensesAvg, @Nullable BigDecimal monthlyIncomeAvg, int monthsWithData) {

    /** VND held in one asset class. */
    public record Holding(AssetClass assetClass, BigDecimal value) {
    }

}
