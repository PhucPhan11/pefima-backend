package com.ceu.pefima.backend.market;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the gold valuation only when a gold quantity is configured, so the
 * app makes no outbound call otherwise.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "pefima.gold", name = "quantity-chi")
@EnableConfigurationProperties(GoldProperties.class)
public class MarketConfiguration {

    @Bean
    GoldPriceClient goldPriceClient(GoldProperties properties) {
        return new PnjGoldPriceClient(MarketHttp.restClient(properties.timeout()), properties.priceUrl(),
                properties.productCode(), Clock.systemUTC());
    }

    @Bean
    GoldValuationService goldValuationService(GoldPriceClient client, GoldProperties properties) {
        return new GoldValuationService(client, properties.quantityChi(), properties.cacheTtl(), Clock.systemUTC());
    }

}
