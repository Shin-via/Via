package com.via.shinvia.login.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm() {
        return "/user/login";
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
