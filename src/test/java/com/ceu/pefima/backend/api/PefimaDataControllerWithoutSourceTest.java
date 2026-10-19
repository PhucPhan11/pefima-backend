package com.ceu.pefima.backend.api;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(PefimaDataController.class)
class PefimaDataControllerWithoutSourceTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void answers503WhenNoSourceIsConfigured() {
        assertThat(this.mvc.get().uri("/api/snapshots")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
            .bodyJson()
            .extractingPath("$.detail")
            .asString()
            .contains("pefima.source.sqlite.path");
        assertThat(this.mvc.get().uri("/api/snapshots/any/transactions"))
            .hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
