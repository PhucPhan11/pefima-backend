package com.ceu.pefima.backend.market;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the exchange rates only when currencies are configured, so the app
 * makes no outbound call otherwise.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "pefima.fx", name = "currencies")
@EnableConfigurationProperties(FxProperties.class)
public class FxConfiguration {

    @Bean
    ExchangeRateClient exchangeRateClient(FxProperties properties) {
        return new VietcombankRateClient(MarketHttp.restClient(properties.timeout()), properties.rateUrl(),
                Clock.systemUTC());
    }

    @Bean
    ExchangeRateService exchangeRateService(ExchangeRateClient client, FxProperties properties) {
        return new ExchangeRateService(client, properties.currencies(), properties.cacheTtl(), Clock.systemUTC());
    }

}
