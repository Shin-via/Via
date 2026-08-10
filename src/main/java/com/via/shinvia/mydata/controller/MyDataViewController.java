package com.via.shinvia.mydata.controller;

import com.via.shinvia.login.security.CustomUserDetails;
import com.via.shinvia.mydata.client.dto.response.CardListResponseDto;
import com.via.shinvia.mydata.service.MyDataCardService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mydata")
@RequiredArgsConstructor
public class MyDataViewController {

    private final MyDataCardService myDataCardService;
    private final CurrentUser currentUser;

    @GetMapping("/result")
    public String resultPage(Authentication authentication, Model model) {
        Long userId=currentUser.getUserId(authentication);
        CardListResponseDto response = myDataCardService.getCards(userId);
        model.addAttribute("cards", response.getCardList());

        return "mydata/result";
    }
}
