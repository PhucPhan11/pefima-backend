package com.ceu.pefima.backend.advisor;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ceu.pefima.backend.advisor.ProfileAnalysis.ScenarioLine;
import com.ceu.pefima.backend.advisor.ProfileAnalysis.ScenarioResult;
import com.ceu.pefima.backend.advisor.ProfileInput.Holding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ProfileCalculatorTest {

    private static ProfileInput input(List<Holding> holdings) {
        return new ProfileInput(holdings, new BigDecimal("50"), new BigDecimal("400"), new BigDecimal("600"),
                new BigDecimal("100"), new BigDecimal("150"), 6);
    }

    private static Holding holding(AssetClass assetClass, String value) {
        return new Holding(assetClass, new BigDecimal(value));
    }

    @Test
    void everyScenarioAddsUpToOneHundred() {
        for (RiskScenario scenario : RiskScenario.values()) {
            assertThat(scenario.targets().values().stream().mapToInt(Integer::intValue).sum()).as(scenario.name())
                .isEqualTo(100);
            assertThat(scenario.targets()).doesNotContainKey(AssetClass.OTHER);
        }
    }

    @Test
    void computesTheCurrentAllocation() {
        ProfileAnalysis analysis = ProfileCalculator.analyse(
                input(List.of(holding(AssetClass.SAFE, "600"), holding(AssetClass.GOLD, "300"),
                        holding(AssetClass.ETF, "100"))));
        assertThat(analysis.totalAssets()).isEqualByComparingTo("1000");
        assertThat(analysis.debts()).isEqualByComparingTo("50");
        assertThat(analysis.current()).hasSize(AssetClass.values().length);
        assertThat(analysis.current().stream().filter((line) -> line.assetClass() == AssetClass.GOLD).findFirst())
            .hasValueSatisfying((line) -> assertThat(line.percent()).isEqualByComparingTo("30"));
        assertThat(analysis.emergencyMonths()).isEqualByComparingTo("6.0");
        assertThat(analysis.ai()).isNull();
        assertThat(analysis.disclaimer()).contains("không phải tư vấn đầu tư");
    }

    @Test
    void comparesEveryScenarioAndCountsOtherAsFund() {
        ProfileAnalysis analysis = ProfileCalculator.analyse(
                input(List.of(holding(AssetClass.SAFE, "500"), holding(AssetClass.FUND, "100"),
                        holding(AssetClass.OTHER, "100"), holding(AssetClass.STOCK, "300"))));
        assertThat(analysis.scenarios()).extracting(ScenarioResult::scenario)
            .containsExactly(RiskScenario.values());
        ScenarioResult balanced = analysis.scenarios().get(RiskScenario.BALANCED.ordinal());
        ScenarioLine fund = balanced.lines()
            .stream()
            .filter((line) -> line.assetClass() == AssetClass.FUND)
            .findFirst()
            .orElseThrow();
        assertThat(fund.currentValue()).isEqualByComparingTo("200");
        assertThat(fund.currentPercent()).isEqualByComparingTo("20");
        assertThat(fund.targetValue()).isEqualByComparingTo("250");
        assertThat(fund.delta()).isEqualByComparingTo("50");
        assertThat(balanced.lines()).extracting(ScenarioLine::assetClass).doesNotContain(AssetClass.OTHER);
        assertThat(balanced.emergencyMonthsTarget()).isEqualTo(6);
    }

    @Test
    void picksTheClosestScenario() {
        ProfileAnalysis conservative = ProfileCalculator.analyse(input(List.of(holding(AssetClass.SAFE, "60"),
                holding(AssetClass.GOLD, "15"), holding(AssetClass.FX, "5"), holding(AssetClass.FUND, "15"),
                holding(AssetClass.ETF, "5"))));
        assertThat(conservative.closest()).isEqualTo(RiskScenario.CONSERVATIVE);
        assertThat(conservative.scenarios().get(0).distance()).isEqualByComparingTo("0");

        ProfileAnalysis aggressive = ProfileCalculator.analyse(input(List.of(holding(AssetClass.STOCK, "70"),
                holding(AssetClass.CRYPTO, "20"), holding(AssetClass.SAFE, "10"))));
        assertThat(aggressive.closest()).isEqualTo(RiskScenario.AGGRESSIVE);
    }

    @Test
    void leavesEmergencyMonthsOutWithoutExpenses() {
        ProfileInput noExpenses = new ProfileInput(List.of(holding(AssetClass.SAFE, "10")), null, null,
                new BigDecimal("10"), null, null, 0);
        assertThat(ProfileCalculator.analyse(noExpenses).emergencyMonths()).isNull();
        assertThat(ProfileCalculator.analyse(noExpenses).debts()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsNegativeOrEmptyHoldings() {
        assertThatExceptionOfType(InvalidProfileException.class)
            .isThrownBy(() -> ProfileCalculator.analyse(input(List.of(holding(AssetClass.SAFE, "-1")))))
            .withMessageContaining("negative");
        assertThatExceptionOfType(InvalidProfileException.class)
            .isThrownBy(() -> ProfileCalculator.analyse(input(List.of(holding(AssetClass.SAFE, "0")))))
            .withMessageContaining("no positive holdings");
    }

}
