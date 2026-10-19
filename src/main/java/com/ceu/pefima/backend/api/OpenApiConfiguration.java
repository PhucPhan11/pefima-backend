package com.ceu.pefima.backend.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Title and description shown at the top of Swagger UI.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI pefimaOpenApi() {
        return new OpenAPI().info(new Info().title("PeFiMa API")
            .version("v0")
            .description("Read-only access to personal finance data collected from pefima-data. "
                    + "Start with GET /api/snapshots, then pass a snapshot ID to the other endpoints."));
    }

}
