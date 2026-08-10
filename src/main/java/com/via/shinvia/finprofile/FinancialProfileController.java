package com.via.shinvia.finprofile;

import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final CurrentUser currentUser;

    @GetMapping
    public String showFinancialProfile(Authentication authentication, Model model) {
        Long userId=currentUser.getUserId(authentication);
        FinancialProfile fprofile= fProfileService.findFinancialProfileByUserId(userId);
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
                                        Authentication authentication){
        Long userId=currentUser.getUserId(authentication);
        fProfileService.createFinancialProfile(request,userId);

        return "redirect:/financial-profile";
    }


    @PostMapping("/edit")
    public String updateFinancialProfile(FinancialProfileRequestDto request,
                                        Authentication authentication){
        Long userId=currentUser.getUserId(authentication);
        fProfileService.updateFinancialProfile(request,userId);

        return "redirect:/financial-profile";
    }
}