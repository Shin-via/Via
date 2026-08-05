package com.via.shinvia.loananalysis.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// ?? ??? ???
@Getter
@Setter
public class LoanAccountAnalysisDTO {

    // ?? ???
    private Long loanAccountId;

    // ?? ??
    private String loanType;

    // ?? ????
    private BigDecimal principalAmount;

    // ?? ????
    private BigDecimal currentBalance;

    // ?? ????
    private BigDecimal interestRate;

    // ???????
    private String rateType;

    // ????
    private String repaymentType;

    // ?? ???
    private LocalDate disbursedAt;

    // ?? ???
    private LocalDate maturityAt;

    // ????????
    private String loanStatus;

    // ????????
    private BigDecimal prepaymentFeeRate;

    // ??????? ???
    private LocalDate prepaymentFeeEndDate;
}