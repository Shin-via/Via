package com.via.shinvia.financialpolicy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/financial-policy")
public class FinancialPolicyController {

    // 스트레스 DSR 안내
    @GetMapping("/stress-dsr")
    public String stressDsr() {
        return "financialpolicy/stress-dsr";
    }

}
