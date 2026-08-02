package com.via.shinvia.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm() {
        return "/member/login";
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
