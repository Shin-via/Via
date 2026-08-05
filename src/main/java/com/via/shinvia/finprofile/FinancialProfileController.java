package com.via.shinvia.finprofile;

import com.via.shinvia.login.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/financial-profile")
@RequiredArgsConstructor
public class FinancialProfileController {
    private final FinancialProfileService fProfileService;

    @GetMapping
    public String showFinancialProfile(@AuthenticationPrincipal CustomUserDetails loginUser, Model model) {
        FinancialProfile fprofile= fProfileService.findFinancialProfileByUserId(loginUser.getUserId());
        model.addAttribute("financialProfile", fprofile);

        if (fprofile == null){
            model.addAttribute("financialProfile", new FinancialProfileRequestDto());
            model.addAttribute("isNew", true);
        } else {
            model.addAttribute("financialProfile", fprofile);
            model.addAttribute("isNew", false);
        }
        return "user/financial-profile";
    }


    @PostMapping("/new")
    public String createFinancialProfile(FinancialProfileRequestDto request,
                                         @AuthenticationPrincipal CustomUserDetails loginUser){
        fProfileService.createFinancialProfile(request,loginUser.getUserId());

        return "redirect:/financial-profile";
    }


    @PostMapping("/edit")
    public String updateFinancialProfile(FinancialProfileRequestDto request,
                                         @AuthenticationPrincipal CustomUserDetails loginUser){
        fProfileService.updateFinancialProfile(request, loginUser.getUserId());

        return "redirect:/financial-profile";
    }

}
