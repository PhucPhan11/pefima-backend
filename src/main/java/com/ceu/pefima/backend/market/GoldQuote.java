package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * One gold product's price, converted to VND per chỉ.
 *
 * @param buyPerChi price the shop pays when you sell (giá mua vào)
 * @param sellPerChi price the shop charges when you buy (giá bán ra)
 * @param quotedAt the feed's own update time, in Vietnam local time
 * @param fetchedAt when the backend fetched the quote
 */
public record GoldQuote(String productCode, String productName, BigDecimal buyPerChi,
        @Nullable BigDecimal sellPerChi, @Nullable LocalDateTime quotedAt, Instant fetchedAt) {
}
