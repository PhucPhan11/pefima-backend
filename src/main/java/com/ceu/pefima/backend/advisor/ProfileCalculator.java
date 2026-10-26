package com.ceu.pefima.backend.advisor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.ceu.pefima.backend.advisor.ProfileAnalysis.AllocationLine;
import com.ceu.pefima.backend.advisor.ProfileAnalysis.ScenarioLine;
import com.ceu.pefima.backend.advisor.ProfileAnalysis.ScenarioResult;

/**
 * Compares a profile with every {@link RiskScenario}. Pure arithmetic: no LLM is
 * involved, so every figure in the analysis is the app's own.
 */
final class ProfileCalculator {

    static final String DISCLAIMER = "Kết quả chỉ để tham khảo, không phải tư vấn đầu tư. Các kịch bản là mẫu "
            + "phân bổ cố định; phần nhận xét do AI chạy trên máy tạo ra.";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ProfileCalculator() {
    }

    /** The comparison without the LLM review. */
    static ProfileAnalysis analyse(ProfileInput input) {
        Map<AssetClass, BigDecimal> values = holdings(input);
        BigDecimal total = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            throw new InvalidProfileException("The profile has no positive holdings");
        }

        List<AllocationLine> current = new ArrayList<>();
        for (AssetClass assetClass : AssetClass.values()) {
            BigDecimal value = values.getOrDefault(assetClass, BigDecimal.ZERO);
            current.add(new AllocationLine(assetClass, assetClass.label(), value, percent(value, total)));
        }

        // Scenarios have no OTHER class; it is compared as FUND.
        Map<AssetClass, BigDecimal> scenarioValues = new EnumMap<>(AssetClass.class);
        values.forEach((assetClass, value) -> scenarioValues.merge(assetClass.scenarioClass(), value, BigDecimal::add));

        List<ScenarioResult> scenarios = new ArrayList<>();
        for (RiskScenario scenario : RiskScenario.values()) {
            List<ScenarioLine> lines = new ArrayList<>();
            BigDecimal distance = BigDecimal.ZERO;
            for (AssetClass assetClass : AssetClass.values()) {
                Integer target = scenario.targets().get(assetClass);
                if (target == null) {
                    continue;
                }
                BigDecimal currentValue = scenarioValues.getOrDefault(assetClass, BigDecimal.ZERO);
                BigDecimal currentPercent = percent(currentValue, total);
                BigDecimal targetPercent = BigDecimal.valueOf(target);
                BigDecimal targetValue = total.multiply(targetPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP);
                lines.add(new ScenarioLine(assetClass, assetClass.label(), targetPercent, currentPercent, targetValue,
                        currentValue, targetValue.subtract(currentValue)));
                distance = distance.add(targetPercent.subtract(currentPercent).abs());
            }
            scenarios.add(new ScenarioResult(scenario, scenario.label(), scenario.description(), List.copyOf(lines),
                    scenario.emergencyMonths(), distance));
        }
        RiskScenario closest = scenarios.stream()
            .min(Comparator.comparing(ScenarioResult::distance))
            .orElseThrow()
            .scenario();

        BigDecimal debts = input.debts() == null ? BigDecimal.ZERO : input.debts().abs();
        return new ProfileAnalysis(total, debts, List.copyOf(current), emergencyMonths(input), List.copyOf(scenarios),
                closest, null, null, DISCLAIMER);
    }

    private static Map<AssetClass, BigDecimal> holdings(ProfileInput input) {
        Map<AssetClass, BigDecimal> values = new EnumMap<>(AssetClass.class);
        if (input.holdings() == null) {
            throw new InvalidProfileException("The profile has no holdings");
        }
        for (ProfileInput.Holding holding : input.holdings()) {
            if (holding.assetClass() == null || holding.value() == null) {
                throw new InvalidProfileException("Every holding needs an asset class and a value");
            }
            if (holding.value().signum() < 0) {
                throw new InvalidProfileException("Holdings cannot be negative; send debts separately");
            }
            values.merge(holding.assetClass(), holding.value(), BigDecimal::add);
        }
        return values;
    }

    private static @Nullable BigDecimal emergencyMonths(ProfileInput input) {
        BigDecimal held = input.emergencyWalletsTotal();
        BigDecimal expenses = input.monthlyExpensesAvg();
        if (held == null || expenses == null || expenses.signum() <= 0) {
            return null;
        }
        return held.divide(expenses, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal value, BigDecimal total) {
        return value.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

}
