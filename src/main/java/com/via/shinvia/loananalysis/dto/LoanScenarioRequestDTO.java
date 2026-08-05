package com.via.shinvia.loananalysis.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// 대출 대안 비교 요청값
@Getter
@Setter
public class LoanScenarioRequestDTO {

    // 회원 식별자
    private Long userId;

    // 분석할 대출 식별자
    private Long targetLoanAccountId;

    // 부분상환 희망금액
    private BigDecimal desiredRepaymentAmount;

    // 반드시 남길 비상자금
    private BigDecimal emergencyFundAmount;

    // 대환 예상금리
    private BigDecimal refinanceInterestRate;

    // 대환 부대비용
    private BigDecimal refinanceCostAmount;

    // 대환 후 상환기간
    private Integer refinancePeriodMonths;
}