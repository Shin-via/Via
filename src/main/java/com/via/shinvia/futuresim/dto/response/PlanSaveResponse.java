package com.via.shinvia.futuresim.dto.response;

import java.math.BigDecimal;

public record PlanSaveResponse(
        Integer baselineMonthsToGoal,
        Integer projectedMonthsToGoal,
        BigDecimal finalNetWorth
) {
}
