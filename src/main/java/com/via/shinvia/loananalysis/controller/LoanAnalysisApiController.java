package com.via.shinvia.loananalysis.controller;

import com.via.shinvia.loananalysis.dto.DebtPriorityResponseDTO;
import com.via.shinvia.loananalysis.service.LoanAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ?? ?? API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loan-analysis")
public class LoanAnalysisApiController {

    private final LoanAnalysisService
            loanAnalysisService;

    // ?? ???? ??
    @GetMapping("/debt-priority/{userId}")
    public ResponseEntity<
            List<DebtPriorityResponseDTO>
            > getDebtPriorities(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                loanAnalysisService
                        .getDebtPriorities(
                                userId
                        )
        );
    }
}