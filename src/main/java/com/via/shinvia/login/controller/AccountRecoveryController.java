package com.via.shinvia.login.controller;

import com.via.shinvia.login.dto.FindLoginEmailRequestDto;
import com.via.shinvia.login.dto.FindLoginEmailResponseDto;
import com.via.shinvia.login.service.AccountRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/find")
@RequiredArgsConstructor
public class AccountRecoveryController {

    private final AccountRecoveryService accountRecoveryService;

    @GetMapping("/id")
    public String findIdForm(Model model) {
        model.addAttribute("findLoginEmailRequest", new FindLoginEmailRequestDto());

        return "user/find-id";
    }

    @PostMapping("/id")
    public String findId(
            @Valid @ModelAttribute("findLoginEmailRequest")
            FindLoginEmailRequestDto request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "user/find-id";
        }

        FindLoginEmailResponseDto result = accountRecoveryService.findLoginEmail(request);

        model.addAttribute("result", result);
        model.addAttribute("searched", true);

        return "user/find-id";
    }
}