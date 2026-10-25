package com.ceu.pefima.backend.market;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * One configured currency's rate in VND per unit.
 *
 * @param rate the rate to value holdings with: the cash buy rate, else the transfer buy rate
 * @param rateType {@code CASH_BUY} or {@code TRANSFER_BUY}, whichever {@code rate} is
 */
public record ExchangeRate(String currencyCode, String currencyName, @Nullable BigDecimal cashBuy,
        @Nullable BigDecimal transferBuy, @Nullable BigDecimal sell, BigDecimal rate, String rateType) {
}
