package com.via.shinvia.loananalysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 대출분석 화면 Controller
@Controller
public class LoanAnalysisViewController {

    // 부채 상환순위 화면
    @GetMapping("/loan-analysis/debt-priority")
    public String debtPriorityPage() {

        return "loananalysis/debt-priority";
    }
    // 대출 대안 비교 화면
    @GetMapping("/loan-analysis/scenarios")
    public String loanScenarioPage() {

        return "loananalysis/loan-scenario";
    }
}