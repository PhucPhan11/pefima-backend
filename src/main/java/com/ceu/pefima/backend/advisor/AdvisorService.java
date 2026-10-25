package com.ceu.pefima.backend.advisor;

import org.jspecify.annotations.Nullable;

import com.ceu.pefima.backend.advisor.ProfileAnalysis.AiReview;

/**
 * Compares a profile with the risk scenarios and, when asked, adds the local
 * LLM's review. The comparison never depends on the LLM: if it fails, the
 * analysis comes back with {@code aiError} instead. Nothing is cached or stored.
 */
public class AdvisorService {

    private final LmStudioClient client;

    AdvisorService(LmStudioClient client) {
        this.client = client;
    }

    public ProfileAnalysis analyse(ProfileInput input, boolean withAi) {
        ProfileAnalysis analysis = ProfileCalculator.analyse(input);
        if (!withAi) {
            return analysis;
        }
        try {
            AiReview review = this.client.review(AdvisorPrompt.SYSTEM, AdvisorPrompt.user(input, analysis));
            return withReview(analysis, review, null);
        }
        catch (AdvisorUnavailableException ex) {
            return withReview(analysis, null, ex.getMessage());
        }
    }

    public String model() {
        return this.client.model();
    }

    private static ProfileAnalysis withReview(ProfileAnalysis analysis, @Nullable AiReview review,
            @Nullable String error) {
        return new ProfileAnalysis(analysis.totalAssets(), analysis.debts(), analysis.current(),
                analysis.emergencyMonths(), analysis.scenarios(), analysis.closest(), review, error,
                analysis.disclaimer());
    }

}
