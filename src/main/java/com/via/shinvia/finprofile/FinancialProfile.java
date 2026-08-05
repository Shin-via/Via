package com.via.shinvia.finprofile;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter @Getter
public class FinancialProfile {
    private Long userFinancialProfileId;
    private Long userId;
    private BigDecimal annualIncome;
    private IncomeType incomeType;
    private EmploymentStatus employmentStatus;
    private Integer creditScore;
    private BigDecimal liquidAssetAmount;   //현금성 자산
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
