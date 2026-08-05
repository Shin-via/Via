package com.via.shinvia.finprofile;

import com.via.shinvia.login.security.CustomUserDetails;
import com.via.shinvia.oauth2.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    public String showFinancialProfile(Authentication authentication, Model model) {
        Long userId=getUserId(authentication);
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
        Long userId=getUserId(authentication);
        fProfileService.createFinancialProfile(request,userId);

        return "redirect:/financial-profile";
    }


    @PostMapping("/edit")
    public String updateFinancialProfile(FinancialProfileRequestDto request,
                                        Authentication authentication){
        Long userId=getUserId(authentication);
        fProfileService.updateFinancialProfile(request,userId);

        return "redirect:/financial-profile";
    }


    private Long getUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("로그인 필요");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        if (principal instanceof CustomOAuth2User oAuth2User) {
            return oAuth2User.getUserId();
        }
        throw new IllegalStateException(
                "지원하지 않는 인증 객체입니다: "
                        + principal.getClass().getName()
        );
    }
}