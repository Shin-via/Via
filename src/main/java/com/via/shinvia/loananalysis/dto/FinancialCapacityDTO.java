package com.via.shinvia.loananalysis.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// ??? ???? ???
@Getter
@Setter
public class FinancialCapacityDTO {

    // ?? ???
    private Long userId;

    // ????? ???
    private Long userFinancialProfileId;

    // ???
    private BigDecimal annualIncome;

    // ????
    private String incomeType;

    // ????
    private String employmentStatus;

    // ????
    private Integer creditScore;

    // ??? ??
    private BigDecimal liquidAssetAmount;

    // ?? ???
    private BigDecimal totalIncome;

    // ?? ???
    private BigDecimal totalExpense;

    // ?? ????
    private BigDecimal savingsAmount;

    // ?? ???
    private BigDecimal savingsRate;

    // ?? ????
    private BigDecimal spendingCv;

    // ?? DSR
    private BigDecimal actualDsr;

    // ???? DSR
    private BigDecimal stressDsr;
}