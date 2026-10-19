package com.ceu.pefima.backend.api;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void publishesOpenApiDocument() {
        assertThat(this.mvc.get().uri("/v3/api-docs")).hasStatusOk()
            .bodyText()
            .contains("PeFiMa API", "/api/snapshots", "/api/snapshots/{snapshotId}/transactions",
                    "/api/snapshots/{snapshotId}/review-issues");
    }

    @Test
    void servesSwaggerUi() {
        assertThat(this.mvc.get().uri("/swagger-ui.html")).hasStatus3xxRedirection();
        assertThat(this.mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }

}
