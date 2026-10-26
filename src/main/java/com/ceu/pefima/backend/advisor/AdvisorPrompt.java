package com.ceu.pefima.backend.advisor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ceu.pefima.backend.advisor.ProfileAnalysis.ScenarioLine;
import com.ceu.pefima.backend.advisor.ProfileAnalysis.ScenarioResult;

/**
 * The chat messages and the JSON schema for the local LLM. The prompt carries the
 * app's computed figures only, rounded to millions of VND, and asks for comments,
 * not new numbers.
 */
final class AdvisorPrompt {

    static final String SYSTEM = """
            Bạn là trợ lý phân tích tài chính cá nhân chạy hoàn toàn trên máy người dùng.
            Quy tắc:
            - Chỉ dùng số liệu được cung cấp; không bịa thêm số, không tự tính tỷ trọng mới.
            - Không khuyên mua hay bán một mã, quỹ hay sản phẩm cụ thể nào.
            - Đây không phải tư vấn đầu tư; viết như nhận xét tham khảo.
            - Viết tiếng Việt, thật ngắn gọn: mỗi nhận xét 1-2 câu, tối đa 3 cảnh báo.
            Trả lời đúng theo JSON schema được yêu cầu.""";

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private AdvisorPrompt() {
    }

    static String user(ProfileInput input, ProfileAnalysis analysis) {
        StringBuilder text = new StringBuilder();
        text.append("Hồ sơ hiện tại (triệu VND, % trên tổng tài sản dương):\n");
        analysis.current()
            .stream()
            .filter((line) -> line.value().signum() > 0)
            .forEach((line) -> text.append("- ")
                .append(line.label())
                .append(": ")
                .append(millions(line.value()))
                .append(" (")
                .append(line.percent().stripTrailingZeros().toPlainString())
                .append("%)\n"));
        text.append("Tổng tài sản dương: ").append(millions(analysis.totalAssets())).append(" triệu\n");
        text.append("Nợ: ").append(millions(analysis.debts())).append(" triệu\n");
        if (input.monthlyExpensesAvg() != null) {
            text.append("Chi tiêu trung bình mỗi tháng: ").append(millions(input.monthlyExpensesAvg())).append(" triệu");
            text.append(" (").append(input.monthsWithData()).append(" tháng có dữ liệu)\n");
        }
        if (input.monthlyIncomeAvg() != null) {
            text.append("Thu nhập trung bình mỗi tháng: ").append(millions(input.monthlyIncomeAvg())).append(" triệu\n");
        }
        if (analysis.emergencyMonths() != null) {
            text.append("Quỹ khẩn cấp đủ cho: ").append(analysis.emergencyMonths().toPlainString()).append(" tháng chi tiêu\n");
        }
        text.append("\nCác kịch bản mẫu (hiện có % → mục tiêu %, rồi cần tăng hay giảm bao nhiêu triệu VND):\n");
        for (ScenarioResult scenario : analysis.scenarios()) {
            text.append("* ")
                .append(scenario.scenario().name())
                .append(" – ")
                .append(scenario.label())
                .append(", quỹ khẩn cấp mục tiêu ")
                .append(scenario.emergencyMonthsTarget())
                .append(" tháng, độ lệch ")
                .append(scenario.distance().stripTrailingZeros().toPlainString())
                .append(" điểm %:\n");
            for (ScenarioLine line : scenario.lines()) {
                // 0% now and 0% in the template says nothing; a shorter prompt is faster on CPU.
                if (line.currentPercent().signum() == 0 && line.targetPercent().signum() == 0) {
                    continue;
                }
                text.append("  - ")
                    .append(line.label())
                    .append(": ")
                    .append(line.currentPercent().stripTrailingZeros().toPlainString())
                    .append("% → ")
                    .append(line.targetPercent().toPlainString())
                    .append("%, ")
                    .append(change(line.delta()))
                    .append('\n');
            }
        }
        text.append("\nKịch bản gần hồ sơ hiện tại nhất: ").append(analysis.closest().name()).append('\n');
        text.append("""

                Hãy viết:
                1. profileSummary: đánh giá hồ sơ hiện tại (đa dạng hoá, tập trung, thanh khoản, quỹ khẩn cấp).
                2. riskReading: hồ sơ này đang gần mức mạo hiểm nào và vì sao.
                3. scenarioComments: với mỗi kịch bản, muốn đi theo hướng đó thì cần dịch chuyển gì, ưu và nhược điểm.
                4. warnings: các điểm cần lưu ý (có thể để trống).""");
        return text.toString();
    }

    /** OpenAI-style {@code response_format} that LM Studio enforces with a grammar. */
    static Map<String, Object> responseFormat() {
        Map<String, Object> comments = new LinkedHashMap<>();
        for (RiskScenario scenario : RiskScenario.values()) {
            comments.put(scenario.name(), Map.of("type", "string"));
        }
        Map<String, Object> schema = Map.of("type", "object", "additionalProperties", false, "required",
                List.of("profileSummary", "riskReading", "scenarioComments", "warnings"), "properties",
                Map.of("profileSummary", Map.of("type", "string"), "riskReading", Map.of("type", "string"),
                        "scenarioComments",
                        Map.of("type", "object", "additionalProperties", false, "required",
                                List.copyOf(comments.keySet()), "properties", comments),
                        "warnings", Map.of("type", "array", "items", Map.of("type", "string"))));
        return Map.of("type", "json_schema", "json_schema",
                Map.of("name", "profile_review", "strict", true, "schema", schema));
    }

    /** "tăng 12.3 triệu", "giảm 4 triệu" or "giữ nguyên", so a small model reads the direction right. */
    private static String change(BigDecimal delta) {
        String amount = millions(delta.abs());
        if (amount.equals("0")) {
            return "giữ nguyên";
        }
        return (delta.signum() > 0 ? "tăng " : "giảm ") + amount + " triệu";
    }

    private static String millions(BigDecimal value) {
        return value.divide(MILLION, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

}
