package com.ceu.pefima.backend.market;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FxConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(FxConfiguration.class);

    @Test
    void registersNothingWithoutCurrencies() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ExchangeRateService.class);
            assertThat(context).doesNotHaveBean(ExchangeRateClient.class);
        });
    }

    @Test
    void registersTheRatesWithVietcombankDefaults() {
        this.contextRunner.withPropertyValues("pefima.fx.currencies=AAA,BBB").run((context) -> {
            assertThat(context).hasSingleBean(ExchangeRateService.class);
            assertThat(context).hasSingleBean(ExchangeRateClient.class);
            FxProperties properties = context.getBean(FxProperties.class);
            assertThat(properties.currencies()).containsExactly("AAA", "BBB");
            assertThat(properties.rateUrl())
                .isEqualTo(URI.create("https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx"));
            assertThat(properties.cacheTtl()).isEqualTo(Duration.ofMinutes(15));
            assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(5));
        });
    }

}
