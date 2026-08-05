package com.via.shinvia.loananalysis.calculator;

import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

// ?? ?? ???? ???
@Component
public class DebtPriorityCalculator {

    // ?? ???
    private static final BigDecimal OVERDUE_WEIGHT =
            new BigDecimal("0.40");

    // ?? ???
    private static final BigDecimal INTEREST_WEIGHT =
            new BigDecimal("0.30");

    // ??? ???
    private static final BigDecimal FEE_WEIGHT =
            new BigDecimal("0.15");

    // ???? ???
    private static final BigDecimal SMALL_LOAN_WEIGHT =
            new BigDecimal("0.10");

    // ????? ???
    private static final BigDecimal STUDENT_LOAN_WEIGHT =
            new BigDecimal("0.05");


    // ?? 1? RPS ??
    public DebtPriorityResponseDTO calculate(
            LoanAccountAnalysisDTO loan,
            BigDecimal totalLoanBalance
    ) {

        // ?? ?? ??
        BigDecimal overdueScore =
                calculateOverdueScore(
                        loan.getLoanStatus()
                );

        // ?? ?? ??
        BigDecimal interestScore =
                defaultZero(
                        loan.getInterestRate()
                );

        // ??? ?? ??
        BigDecimal feeScore =
                calculateFeeScore(
                        loan.getPrepaymentFeeRate(),
                        loan.getPrepaymentFeeEndDate()
                );

        // ???? ?? ??
        BigDecimal smallLoanScore =
                calculateSmallLoanScore(
                        loan.getCurrentBalance(),
                        totalLoanBalance
                );

        // ????? ?? ??
        BigDecimal studentLoanScore =
                calculateStudentLoanScore(
                        loan.getLoanType()
                );


        // ?? ????
        BigDecimal weightedOverdue =
                overdueScore.multiply(
                        OVERDUE_WEIGHT
                );

        // ?? ????
        BigDecimal weightedInterest =
                interestScore.multiply(
                        INTEREST_WEIGHT
                );

        // ??? ????
        BigDecimal weightedFee =
                feeScore.multiply(
                        FEE_WEIGHT
                );

        // ???? ????
        BigDecimal weightedSmallLoan =
                smallLoanScore.multiply(
                        SMALL_LOAN_WEIGHT
                );

        // ????? ????
        BigDecimal weightedStudentLoan =
                studentLoanScore.multiply(
                        STUDENT_LOAN_WEIGHT
                );


        // ?? RPS ??
        BigDecimal finalScore =
                weightedOverdue
                        .add(weightedInterest)
                        .subtract(weightedFee)
                        .add(weightedSmallLoan)
                        .subtract(weightedStudentLoan)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        // ?? ?? ??
        String reason =
                createReason(
                        loan,
                        overdueScore,
                        feeScore,
                        smallLoanScore,
                        studentLoanScore
                );


        // ?? ?? ??
        return DebtPriorityResponseDTO.builder()
                .loanAccountId(
                        loan.getLoanAccountId()
                )
                .loanType(
                        loan.getLoanType()
                )
                .currentBalance(
                        defaultZero(
                                loan.getCurrentBalance()
                        )
                )
                .interestRate(
                        interestScore
                )
                .rateType(
                        loan.getRateType()
                )
                .loanStatus(
                        loan.getLoanStatus()
                )
                .overdueScore(
                        overdueScore
                )
                .interestScore(
                        interestScore
                )
                .feeScore(
                        feeScore
                )
                .smallLoanScore(
                        smallLoanScore
                )
                .studentLoanScore(
                        studentLoanScore
                )
                .priorityScore(
                        finalScore
                )
                .reason(
                        reason
                )
                .build();
    }


    // ?? ?? ??
    private BigDecimal calculateOverdueScore(
            String loanStatus
    ) {

        // ???? 100?
        if ("??".equals(loanStatus)) {
            return BigDecimal.valueOf(100);
        }

        // ???? 0?
        return BigDecimal.ZERO;
    }


    // ??????? ?? ??
    private BigDecimal calculateFeeScore(
            BigDecimal feeRate,
            LocalDate feeEndDate
    ) {

        // ???? ??
        if (feeRate == null
                || feeRate.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            return BigDecimal.ZERO;
        }

        // ??? ??
        if (feeEndDate == null) {
            return feeRate.multiply(
                    BigDecimal.TEN
            );
        }

        // ??? ?? ??
        if (feeEndDate.isBefore(
                LocalDate.now()
        )) {
            return BigDecimal.ZERO;
        }

        // ???? ? 10
        return feeRate.multiply(
                BigDecimal.TEN
        );
    }


    // ???? ?? ??
    private BigDecimal calculateSmallLoanScore(
            BigDecimal currentBalance,
            BigDecimal totalLoanBalance
    ) {

        // ???? null ??
        BigDecimal balance =
                defaultZero(currentBalance);

        // ????? null ??
        BigDecimal totalBalance =
                defaultZero(totalLoanBalance);

        // ???? 0?? ?? ??
        if (totalBalance.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            return BigDecimal.ZERO;
        }

        // ?? ?? ?? ??
        BigDecimal balanceRatio =
                balance.divide(
                        totalBalance,
                        10,
                        RoundingMode.HALF_UP
                );

        // 100 ? (1 - ?? ??)
        BigDecimal score =
                BigDecimal.ONE
                        .subtract(balanceRatio)
                        .multiply(
                                BigDecimal.valueOf(100)
                        );

        // ?? ?? ? ??? ??
        return score
                .max(BigDecimal.ZERO)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // ????? ?? ??
    private BigDecimal calculateStudentLoanScore(
            String loanType
    ) {

        // ?? ?? ??
        if (loanType == null) {
            return BigDecimal.ZERO;
        }

        // ??????? 100?
        if (loanType.contains("???")) {
            return BigDecimal.valueOf(100);
        }

        // ?????? 0?
        return BigDecimal.ZERO;
    }


    // ?? ?? ??
    private String createReason(
            LoanAccountAnalysisDTO loan,
            BigDecimal overdueScore,
            BigDecimal feeScore,
            BigDecimal smallLoanScore,
            BigDecimal studentLoanScore
    ) {

        StringBuilder reason =
                new StringBuilder();

        // ?? ??
        if (overdueScore.compareTo(
                BigDecimal.ZERO
        ) > 0) {
            reason.append(
                    "?? ?? ???? ??? ??? ?????. "
            );
        }

        // ??? ??
        if (defaultZero(
                loan.getInterestRate()
        ).compareTo(
                BigDecimal.valueOf(7)
        ) >= 0) {

            reason.append(
                    "??? ?? ?????. "
            );

            // ??? ??
        } else if (defaultZero(
                loan.getInterestRate()
        ).compareTo(
                BigDecimal.valueOf(5)
        ) >= 0) {

            reason.append(
                    "??? ?????. "
            );
        }

        // ??? ??
        if (feeScore.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            reason.append(
                    "???????? ?? ?? ??? ??? ???? ???. "
            );
        }

        // ???? ??
        if (smallLoanScore.compareTo(
                BigDecimal.valueOf(80)
        ) >= 0) {

            reason.append(
                    "?? ???? ???? ??? ?? ?? ??? ?????. "
            );
        }

        // ????? ??
        if (studentLoanScore.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            reason.append(
                    "?????? ????? ?? ????? ?? ??????. "
            );
        }

        // ?? ??
        if (reason.isEmpty()) {
            reason.append(
                    "??? ?? ??? ???? ??? ??????."
            );
        }

        return reason.toString().trim();
    }


    // null ?? ??
    private BigDecimal defaultZero(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}