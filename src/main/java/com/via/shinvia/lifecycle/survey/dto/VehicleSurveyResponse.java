package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSurveyResponse {

    // 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 시나리오 식별자
    private Long lifecycleScenarioId;

    // 이벤트 실행 순서
    private Integer eventOrder;

    // 차량 구매 예정일
    private LocalDate targetDate;

    // 차량 구매 수준
    // PRACTICAL, AVERAGE, RELAXED, PREMIUM, CUSTOM
    private LifestyleLevel lifestyleLevel;

    // 차량 상태
    // NEW = 신차
    // USED = 중고차
    private String vehicleCondition;

    // 차량 등급
    // 경차, 소형, 준중형, 중형, 준대형, SUV 등
    private String vehicleClass;

    // 차량 구매에 사용할 현금
    private BigDecimal cashPaymentAmount;

    // 차량 구매를 위해 받을 예정인 대출금액
    private BigDecimal loanAmount;

    // 자동차대출 기간
    // 단위: 개월
    private Integer loanPeriodMonths;

    // 연간 예상 주행거리
    // 향후 유류비 계산 시 사용
    private Integer annualMileage;

    // CUSTOM 선택 시 사용자가 직접 입력한 차량가격
    private BigDecimal customVehiclePrice;

    // 이벤트 최초 생성일시
    private LocalDateTime createdAt;

    // 이벤트 마지막 수정일시
    private LocalDateTime updatedAt;
}