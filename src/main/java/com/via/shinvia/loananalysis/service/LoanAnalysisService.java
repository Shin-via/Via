package com.via.shinvia.loananalysis.service;

import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioRequestDTO;
import com.via.shinvia.loananalysis.dto.LoanScenarioResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 대출분석 통합 서비스
@Service
@RequiredArgsConstructor
public class LoanAnalysisService {

    private final DebtPriorityService
            debtPriorityService;

    private final LoanScenarioService
            loanScenarioService;


    // 부채 상환순위 조회
    public List<DebtPriorityResponseDTO>
    getDebtPriorities(
            Long userId
    ) {
        return debtPriorityService.calculate(
                userId
        );
    }


    // 대출 대안 비교
    public List<LoanScenarioResponseDTO>
    analyzeScenarios(
            LoanScenarioRequestDTO request
    ) {
        return loanScenarioService.calculate(
                request
        );
    }
}