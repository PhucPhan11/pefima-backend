package com.ceu.pefima.backend.advisor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Fixed reference allocations by risk level, chosen by the user on 2026-10-25 so
 * that the figures are never made up by the LLM. They are generic illustrations,
 * not investment advice. Each scenario's percentages add up to 100.
 */
public enum RiskScenario {

    CONSERVATIVE("Thận trọng", "Giữ vốn là chính, chấp nhận lợi nhuận thấp.", 12,
            allocation(60, 15, 5, 15, 5, 0, 0)),

    BALANCED("Cân bằng", "Cân bằng giữa an toàn và tăng trưởng.", 6, allocation(40, 10, 5, 25, 15, 5, 0)),

    GROWTH("Tăng trưởng", "Ưu tiên tăng trưởng dài hạn, chịu được biến động.", 6,
            allocation(25, 10, 5, 25, 25, 10, 0)),

    AGGRESSIVE("Mạo hiểm", "Tối đa tăng trưởng, chấp nhận sụt giảm mạnh.", 3,
            allocation(15, 5, 0, 20, 30, 25, 5));

    private final String label;

    private final String description;

    private final int emergencyMonths;

    private final Map<AssetClass, Integer> targets;

    RiskScenario(String label, String description, int emergencyMonths, Map<AssetClass, Integer> targets) {
        this.label = label;
        this.description = description;
        this.emergencyMonths = emergencyMonths;
        this.targets = targets;
    }

    public String label() {
        return this.label;
    }

    public String description() {
        return this.description;
    }

    /** Months of average expenses the emergency fund should cover. */
    public int emergencyMonths() {
        return this.emergencyMonths;
    }

    /** Target percent per scenario class (every class but {@link AssetClass#OTHER}). */
    public Map<AssetClass, Integer> targets() {
        return this.targets;
    }

    private static Map<AssetClass, Integer> allocation(int safe, int gold, int fx, int fund, int etf, int stock,
            int crypto) {
        Map<AssetClass, Integer> targets = new EnumMap<>(AssetClass.class);
        targets.put(AssetClass.SAFE, safe);
        targets.put(AssetClass.GOLD, gold);
        targets.put(AssetClass.FX, fx);
        targets.put(AssetClass.FUND, fund);
        targets.put(AssetClass.ETF, etf);
        targets.put(AssetClass.STOCK, stock);
        targets.put(AssetClass.CRYPTO, crypto);
        return Map.copyOf(targets);
    }

}
