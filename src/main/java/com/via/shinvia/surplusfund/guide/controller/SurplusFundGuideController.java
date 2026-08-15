package com.via.shinvia.surplusfund.guide.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SurplusFundGuideController {

    @GetMapping("/surplus-funds/guide")
    public String guide() {
        return "surplusfund/guide";
    }
}
