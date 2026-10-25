package com.ceu.pefima.backend.market;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Gold holding and the price feed used to value it. Keep the real quantity in
 * the git-ignored {@code pefima-local.properties}.
 *
 * @param quantityChi gold held, in chỉ (1 lượng = 10 chỉ)
 * @param productCode product code ({@code masp}) in the PNJ price list; {@code N24K} is "Nhẫn Trơn PNJ 999.9"
 * @param priceUrl PNJ gold price endpoint
 * @param cacheTtl how long a fetched quote is reused before PNJ is asked again
 * @param timeout connect and read timeout for the price request
 */
@ConfigurationProperties("pefima.gold")
public record GoldProperties(BigDecimal quantityChi, @DefaultValue("N24K") String productCode,
        @DefaultValue("https://edge-api.pnj.io/ecom-frontend/v1/get-gold-price?zone=00") URI priceUrl,
        @DefaultValue("15m") Duration cacheTtl, @DefaultValue("5s") Duration timeout) {
}
