package com.via.shinvia.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReportHeaderResponse(
        BigDecimal totalScore,
        String grade,
        List<Dimension> dimensions
) {
    public record Dimension(String key, String label, BigDecimal score, BigDecimal weightPercent) {
    }
}
