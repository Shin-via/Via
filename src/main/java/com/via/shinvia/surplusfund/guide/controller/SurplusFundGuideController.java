package com.via.shinvia.surplusfund.guide.controller;

import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.surplusfund.calculation.service.SurplusFundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class SurplusFundGuideController {
    private final CurrentUser currentUser;
    private final SurplusFundService surplusFundService;

    @GetMapping("/surplus-funds/guide")
    public String guide(Authentication authentication, Model model) {
        Long userId = currentUser.getUserId(authentication);

        model.addAttribute("totalCurrentBalance", surplusFundService.calculateTotalCurrentBalance(userId));

        return "surplusfund/guide";
    }
}
