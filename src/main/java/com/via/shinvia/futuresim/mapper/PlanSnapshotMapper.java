package com.via.shinvia.futuresim.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface PlanSnapshotMapper {

    // 사용자당 1행(uk_user_plan) — 있으면 덮어쓰고 없으면 새로 만든다.
    void upsert(
            @Param("appUserId") Long appUserId,
            @Param("goalAmount") BigDecimal goalAmount,
            @Param("goalPresetKey") String goalPresetKey,
            @Param("selectedLeversJson") String selectedLeversJson,
            @Param("baselineMonthsToGoal") int baselineMonthsToGoal,
            @Param("projectedMonthsToGoal") int projectedMonthsToGoal,
            @Param("finalNetWorth") BigDecimal finalNetWorth
    );
}
