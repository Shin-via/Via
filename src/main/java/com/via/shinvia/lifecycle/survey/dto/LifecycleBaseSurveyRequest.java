package com.via.shinvia.lifecycle.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleBaseSurveyRequest {

    // 사용자의 현재 월평균 생활비
    // 식비, 교통비, 통신비 등 일반적인 생활비
    private BigDecimal monthlyLivingExpense;

    // 현재 주거형태
    // FAMILY : 가족과 거주
    // MONTHLY_RENT : 월세
    // JEONSE : 전세
    // OWN : 자가
    private String currentHousingType;

    // 현재 매월 발생하는 주거비
    // 월세 + 관리비 등을 합친 월 기준 금액
    // 가족과 거주하거나 자가이고 별도 주거비가 없으면 0 가능
    private BigDecimal monthlyHousingExpense;

    // 사용자의 산업군
    // 미래 소득 상승률 기준자료 조회 시 사용
    // 예: IT, FINANCE, MANUFACTURING 등
    private String industryCode;

    // 미래 급여상승 시나리오
    // CONSERVATIVE : 보수적
    // BASE : 기준
    // OPTIMISTIC : 낙관적
    // CUSTOM : 직접입력
    private String salaryGrowthScenario;

    // 사용자가 CUSTOM을 선택했을 경우 직접 입력한 연평균 급여상승률
    // 예: 0.03 = 연 3%
    // CUSTOM이 아니면 null 가능
    private BigDecimal customSalaryGrowthRate;
}