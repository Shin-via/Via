package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.VehicleCondition;
import com.via.shinvia.lifecycle.common.model.VehicleClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSurveyRequest {

    // 차량 구매 예정일
    private LocalDate targetDate;

    // 차량 가격 수준
    private LifestyleLevel lifestyleLevel;

    // NEW : 신차
    // USED : 중고차
    private VehicleCondition vehicleCondition;

    // 경차, 소형, 준중형, 중형, 준대형, SUV 등
    private VehicleClass vehicleClass;

    // 구매 시 사용할 현금
    private BigDecimal cashPaymentAmount;

    // 차량 구매 시 이용할 대출 예정금액
    private BigDecimal loanAmount;

    // 자동차대출 기간
    private Integer loanPeriodMonths;

    // 연간 예상 주행거리
    // 유류비 계산 시 사용 가능
    private Integer annualMileage;

    // CUSTOM 선택 시 직접 입력한 차량가격
    private BigDecimal customVehiclePrice;
}
