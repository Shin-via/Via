package com.via.shinvia.loananalysis.service;

import com.via.shinvia.loananalysis.calculator.DebtPriorityCalculator;
import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import com.via.shinvia.loananalysis.mapper.LoanAccountAnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// ?? ?????? ???
@Service
@RequiredArgsConstructor
public class DebtPriorityService {

    private final LoanAccountAnalysisMapper
            loanAccountAnalysisMapper;

    private final DebtPriorityCalculator
            debtPriorityCalculator;


    // ??? ?? ???? ??
    public List<DebtPriorityResponseDTO> calculate(
            Long userId
    ) {
        // ??? ?? ??
        List<LoanAccountAnalysisDTO> loans =
                loanAccountAnalysisMapper
                        .findActiveLoansByUserId(
                                userId
                        );

        // ?? ?? ??
        if (loans == null || loans.isEmpty()) {
            return List.of();
        }

        // ?? ???? ??
        BigDecimal totalLoanBalance =
                loans.stream()
                        .map(
                                LoanAccountAnalysisDTO
                                        ::getCurrentBalance
                        )
                        .filter(
                                balance -> balance != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        // ??? RPS ??
        List<DebtPriorityResponseDTO> results =
                loans.stream()
                        .map(loan ->
                                debtPriorityCalculator
                                        .calculate(
                                                loan,
                                                totalLoanBalance
                                        )
                        )
                        .sorted(
                                Comparator.comparing(
                                        DebtPriorityResponseDTO
                                                ::getPriorityScore
                                ).reversed()
                        )
                        .toList();

        // ?? ?? ??
        List<DebtPriorityResponseDTO> rankedResults =
                new ArrayList<>();

        for (int index = 0;
             index < results.size();
             index++) {

            DebtPriorityResponseDTO result =
                    results.get(index);

            // ?? ?? ?? ??
            rankedResults.add(
                    DebtPriorityResponseDTO.builder()
                            .loanAccountId(
                                    result.getLoanAccountId()
                            )
                            .loanType(
                                    result.getLoanType()
                            )
                            .currentBalance(
                                    result.getCurrentBalance()
                            )
                            .interestRate(
                                    result.getInterestRate()
                            )
                            .rateType(
                                    result.getRateType()
                            )
                            .loanStatus(
                                    result.getLoanStatus()
                            )
                            .overdueScore(
                                    result.getOverdueScore()
                            )
                            .interestScore(
                                    result.getInterestScore()
                            )
                            .feeScore(
                                    result.getFeeScore()
                            )
                            .smallLoanScore(
                                    result.getSmallLoanScore()
                            )
                            .studentLoanScore(
                                    result.getStudentLoanScore()
                            )
                            .priorityScore(
                                    result.getPriorityScore()
                            )
                            .priorityRank(
                                    index + 1
                            )
                            .reason(
                                    result.getReason()
                            )
                            .build()
            );
        }

        return rankedResults;
    }
}