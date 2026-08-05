package com.via.shinvia.loananalysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// ???? ?? Controller
@Controller
public class LoanAnalysisViewController {

    // ?? ???? ??
    @GetMapping("/loan-analysis/debt-priority")
    public String debtPriorityPage() {

        return "loananalysis/debt-priority";
    }
}