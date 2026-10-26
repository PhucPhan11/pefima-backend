package com.ceu.pefima.backend.advisor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The profile compared with every risk scenario, plus the local LLM's review
 * when it was asked for and answered.
 *
 * @param totalAssets sum of the holdings (debts not subtracted)
 * @param current every asset class with its value and percent of {@code totalAssets}
 * @param emergencyMonths months of average expenses the emergency wallets cover, or
 * {@code null} without expense data
 * @param closest the scenario nearest to the current allocation (smallest L1 distance)
 * @param aiError why the review is missing when it was asked for; value-free
 */
public record ProfileAnalysis(BigDecimal totalAssets, BigDecimal debts, List<AllocationLine> current,
        @Nullable BigDecimal emergencyMonths, List<ScenarioResult> scenarios, RiskScenario closest,
        @Nullable AiReview ai, @Nullable String aiError, String disclaimer) {

    /** One asset class in the current profile. */
    public record AllocationLine(AssetClass assetClass, String label, BigDecimal value, BigDecimal percent) {
    }

    /**
     * One scenario against the current profile.
     *
     * @param distance sum of the absolute percent differences over all classes
     */
    public record ScenarioResult(RiskScenario scenario, String label, String description, List<ScenarioLine> lines,
            int emergencyMonthsTarget, BigDecimal distance) {
    }

    /**
     * One asset class in a scenario. {@code delta} is target − current, in VND: positive
     * means add to that class.
     */
    public record ScenarioLine(AssetClass assetClass, String label, BigDecimal targetPercent,
            BigDecimal currentPercent, BigDecimal targetValue, BigDecimal currentValue, BigDecimal delta) {
    }

    /** The local LLM's comments; text only, the figures above are the app's. */
    public record AiReview(String profileSummary, String riskReading, Map<RiskScenario, String> scenarioComments,
            List<String> warnings, String model) {
    }

}
