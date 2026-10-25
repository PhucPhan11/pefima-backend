package com.ceu.pefima.backend.market;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        RestClient restClient = RestClient.builder()
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; pefima-backend)")
            .build();
        return new PnjGoldPriceClient(restClient, properties.priceUrl(), properties.productCode(),
                Clock.systemUTC());
    }

    @Bean
    GoldValuationService goldValuationService(GoldPriceClient client, GoldProperties properties) {
        return new GoldValuationService(client, properties.quantityChi(), properties.cacheTtl(), Clock.systemUTC());
    }

}
