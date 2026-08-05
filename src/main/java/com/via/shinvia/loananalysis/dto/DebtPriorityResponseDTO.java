package com.via.shinvia.loananalysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// ?? ???? ?? DTO
@Getter
@Builder
public class DebtPriorityResponseDTO {

    // ?? ???
    private Long loanAccountId;

    // ?? ??
    private String loanType;

    // ?? ????
    private BigDecimal currentBalance;

    // ?? ??
    private BigDecimal interestRate;

    // ?? ??
    private String rateType;

    // ?? ??
    private String loanStatus;

    // ?? ???
    private BigDecimal overdueScore;

    // ?? ???
    private BigDecimal interestScore;

    // ??????? ???
    private BigDecimal feeScore;

    // ???? ???
    private BigDecimal smallLoanScore;

    // ????? ???
    private BigDecimal studentLoanScore;

    // ?? RPS ??
    private BigDecimal priorityScore;

    // ?? ????
    private Integer priorityRank;

    // ?? ??
    private String reason;
}