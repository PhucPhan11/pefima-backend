package com.ceu.pefima.backend.advisor;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisorConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AdvisorConfiguration.class);

    @Test
    void registersNothingUnlessEnabled() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AdvisorService.class);
        });
    }

    @Test
    void registersTheAdvisorWithLmStudioDefaults() {
        this.contextRunner.withPropertyValues("pefima.advisor.enabled=true").run((context) -> {
            assertThat(context).hasSingleBean(AdvisorService.class);
            AdvisorProperties properties = context.getBean(AdvisorProperties.class);
            assertThat(properties.baseUrl()).isEqualTo(URI.create("http://127.0.0.1:1234"));
            assertThat(properties.model()).isEqualTo("google/gemma-4-e4b");
            assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(285));
            assertThat(properties.maxTokens()).isEqualTo(600);
            assertThat(context.getBean(AdvisorService.class).model()).isEqualTo("google/gemma-4-e4b");
        });
    }

    @Test
    void acceptsLocalhost() {
        this.contextRunner
            .withPropertyValues("pefima.advisor.enabled=true", "pefima.advisor.base-url=http://localhost:1234")
            .run((context) -> assertThat(context).hasNotFailed());
    }

    @Test
    void refusesAnLlmOffThisMachine() {
        this.contextRunner
            .withPropertyValues("pefima.advisor.enabled=true", "pefima.advisor.base-url=http://192.0.2.10:1234")
            .run((context) -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("stay local");
            });
    }

}
