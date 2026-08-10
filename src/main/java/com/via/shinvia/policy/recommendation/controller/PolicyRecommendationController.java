package com.via.shinvia.policy.recommendation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policy/recommendation")
public class PolicyRecommendationController {


    // 맞춤 금융지원상품 설문
    @GetMapping
    public String recommendationForm() {

        return "policy/recommendation/recommendation-form";
    }


    // 추천 결과 화면
    @GetMapping("/result")
    public String recommendationResult() {

        return "policy/recommendation/recommendation-result";
    }
}