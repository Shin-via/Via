package com.via.shinvia.futuresim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.via.shinvia.futuresim.mapper.PlanSnapshotMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// 5단계("레버 조합해보기")에서 "다음" 클릭 시 현재 계획을 저장한다. 사용자당 1행만 유지하고
// (futuresim_plan_snapshot.uk_user_plan), 다시 저장하면 upsert로 덮어쓴다 — 계획을 여러 번 바꿔도
// 행이 쌓이지 않는다.
@Service
public class PlanSnapshotService {

    private final PlanSnapshotMapper mapper;
    // 이 프로젝트(Spring Boot 4)에는 com.fasterxml.jackson.databind.ObjectMapper 빈이 자동 등록돼 있지
    // 않아서(Jackson 3 계열만 auto-configure됨), 여기서는 직접 만들어 쓴다 — 레코드 몇 개를 JSON 배열로
    // 바꾸는 단순한 용도라 커스텀 모듈 등록 없이 기본 설정으로 충분하다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlanSnapshotService(PlanSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    public void save(
            Long userId,
            BigDecimal goalAmount,
            String goalPresetKey,
            List<LeverIntensityCalculator.LeverSelection> selections,
            int baselineMonthsToGoal,
            int projectedMonthsToGoal,
            BigDecimal finalNetWorth
    ) {
        mapper.upsert(
                userId, goalAmount, goalPresetKey, toJson(selections),
                baselineMonthsToGoal, projectedMonthsToGoal, finalNetWorth
        );
    }

    private String toJson(List<LeverIntensityCalculator.LeverSelection> selections) {
        try {
            return objectMapper.writeValueAsString(selections);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("선택한 레버 목록을 JSON으로 변환하지 못했습니다", e);
        }
    }
}
