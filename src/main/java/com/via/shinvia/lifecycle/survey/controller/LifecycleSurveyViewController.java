package com.via.shinvia.lifecycle.survey.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LifecycleSurveyViewController {

    /**
     * 생애주기 설문 화면
     */
    @GetMapping("/lifecycle/survey")
    public String lifecycleSurvey() {

        // templates/lifecycle/lifecycle-survey.html 반환
        return "lifecycle/lifecycle-survey";
    }
}