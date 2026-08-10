package com.via.shinvia.mydata.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mydata")
@RequiredArgsConstructor
public class MyDataViewController {

    @GetMapping("/result")
    public String resultPage() {
        return "mydata/result";
    }
}
