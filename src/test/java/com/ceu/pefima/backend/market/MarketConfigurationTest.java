package com.ceu.pefima.backend.market;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MarketConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MarketConfiguration.class);

    @Test
    void registersNothingWithoutAQuantity() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(GoldValuationService.class);
            assertThat(context).doesNotHaveBean(GoldPriceClient.class);
        });
    }

    @Test
    void registersTheValuationWithPnjDefaults() {
        this.contextRunner.withPropertyValues("pefima.gold.quantity-chi=2.5").run((context) -> {
            assertThat(context).hasSingleBean(GoldValuationService.class);
            assertThat(context).hasSingleBean(GoldPriceClient.class);
            GoldProperties properties = context.getBean(GoldProperties.class);
            assertThat(properties.quantityChi()).isEqualByComparingTo("2.5");
            assertThat(properties.productCode()).isEqualTo("N24K");
            assertThat(properties.priceUrl())
                .isEqualTo(URI.create("https://edge-api.pnj.io/ecom-frontend/v1/get-gold-price?zone=00"));
            assertThat(properties.cacheTtl()).isEqualTo(Duration.ofMinutes(15));
            assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(5));
        });
    }

}
