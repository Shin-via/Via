package com.via.shinvia.loananalysis.service;

import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// ?? ?? ?? ???
@Service
@RequiredArgsConstructor
public class LoanAnalysisService {

    private final DebtPriorityService
            debtPriorityService;

    // ?? ???? ??
    public List<DebtPriorityResponseDTO>
    getDebtPriorities(
            Long userId
    ) {
        return debtPriorityService.calculate(
                userId
        );
    }
}