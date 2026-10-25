package com.ceu.pefima.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ceu.pefima.backend.advisor.AdvisorService;
import com.ceu.pefima.backend.advisor.ProfileAnalysis;
import com.ceu.pefima.backend.advisor.ProfileInput;

/**
 * Investment profile review. A POST only because the profile travels in the body:
 * nothing is written or stored.
 */
@RestController
@RequestMapping("/api/advisor")
@Tag(name = "Advisor", description = "Risk scenarios and a local LLM review of an aggregated profile")
class AdvisorController {

    private final ObjectProvider<AdvisorService> advisor;

    AdvisorController(ObjectProvider<AdvisorService> advisor) {
        this.advisor = advisor;
    }

    @PostMapping("/analysis")
    @Operation(summary = "Compare a profile with the risk scenarios",
            description = "Takes totals per asset class (no wallet names). Returns the current allocation, four "
                    + "fixed reference scenarios with the change each needs, and with ai=true a review written by "
                    + "the local LLM in LM Studio. Not investment advice; nothing is stored.")
    ProfileAnalysis analyse(@RequestBody ProfileInput input,
            @RequestParam(defaultValue = "false") @Parameter(description = "Ask the local LLM for a review") boolean ai) {
        AdvisorService service = this.advisor.getIfAvailable();
        if (service == null) {
            throw new SourceNotConfiguredException("The advisor is not configured. Set pefima.advisor.enabled=true "
                    + "(for example in pefima-local.properties) and restart.");
        }
        return service.analyse(input, ai);
    }

}
