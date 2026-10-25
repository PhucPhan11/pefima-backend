package com.ceu.pefima.backend.market;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Foreign currencies to price and the rate feed used for them. Keep the real
 * list in the git-ignored {@code pefima-local.properties}.
 *
 * @param currencies ISO codes to return, such as {@code THB,JPY}
 * @param rateUrl Vietcombank exchange-rate XML endpoint
 * @param cacheTtl how long a fetched rate list is reused; at least 5 minutes, which the feed asks for
 * @param timeout connect and read timeout for the rate request
 */
@ConfigurationProperties("pefima.fx")
public record FxProperties(List<String> currencies,
        @DefaultValue("https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx") URI rateUrl,
        @DefaultValue("15m") Duration cacheTtl, @DefaultValue("5s") Duration timeout) {
}
