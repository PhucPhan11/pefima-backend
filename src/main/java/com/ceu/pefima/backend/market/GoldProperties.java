package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The gold price feed, on only when {@code enabled} is true. The holding itself
 * is the Gold wallet's balance in Notion, in chỉ, so {@code quantityChi} is optional.
 *
 * @param enabled turns the feed on; set it in the git-ignored {@code pefima-local.properties}
 * @param quantityChi optional gold held, in chỉ (1 lượng = 10 chỉ); without it the
 * valuation carries the price only
 * @param productCode product code ({@code masp}) in the PNJ price list; {@code N24K} is "Nhẫn Trơn PNJ 999.9"
 * @param priceUrl PNJ gold price endpoint
 * @param cacheTtl how long a fetched quote is reused before PNJ is asked again
 * @param timeout connect and read timeout for the price request
 */
@ConfigurationProperties("pefima.gold")
public record GoldProperties(@DefaultValue("false") boolean enabled, @Nullable BigDecimal quantityChi,
        @DefaultValue("N24K") String productCode,
        @DefaultValue("https://edge-api.pnj.io/ecom-frontend/v1/get-gold-price?zone=00") URI priceUrl,
        @DefaultValue("15m") Duration cacheTtl, @DefaultValue("5s") Duration timeout) {
}
