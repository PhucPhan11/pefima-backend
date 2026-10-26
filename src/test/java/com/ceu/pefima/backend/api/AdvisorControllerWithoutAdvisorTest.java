package com.ceu.pefima.backend.api;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(AdvisorController.class)
class AdvisorControllerWithoutAdvisorTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void answers503WhenTheAdvisorIsNotEnabled() {
        assertThat(this.mvc.post()
            .uri("/api/advisor/analysis")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"holdings\":[],\"monthsWithData\":0}")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
            .bodyJson()
            .extractingPath("$.detail")
            .asString()
            .contains("pefima.advisor.enabled");
    }

}
