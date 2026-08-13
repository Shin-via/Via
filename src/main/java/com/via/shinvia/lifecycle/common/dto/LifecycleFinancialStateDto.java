package com.via.shinvia.lifecycle.common.dto;

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
public class LifecycleFinancialStateDto {

    // 현재 금융상태가 어느 시점 기준인지
    private LocalDate stateDate;

    // 현재 현금성 자산
    private BigDecimal cashAsset;

    // 현재 투자자산
    private BigDecimal investmentAsset;

    // 현재 보유 주택 자산가치
    private BigDecimal housingAsset;

    // 현재 보유 차량 자산가치
    private BigDecimal vehicleAsset;

    // 현재 전체 부채잔액
    private BigDecimal totalDebt;

    // 해당 시점의 예상 연소득
    private BigDecimal annualIncome;

    // 해당 시점의 월 생활비
    private BigDecimal monthlyLivingExpense;

    // 해당 시점의 월 주거비
    private BigDecimal monthlyHousingExpense;

    // 해당 시점의 월 대출 상환액
    private BigDecimal monthlyDebtPayment;

    // 해당 시점에 매월 들어오는 정부지원금
    private BigDecimal monthlySupportIncome;

    // 월소득에서 생활비, 주거비, 대출상환액 등을 제외한
    // 예상 월 저축 가능금액
    private BigDecimal monthlySavingCapacity;

    // 해당 시점의 DSR
    // 예: 0.35 = 35%
    private BigDecimal dsr;
}