package com.via.shinvia.lifecycle.survey.controller;

import com.via.shinvia.lifecycle.common.model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LifecycleSurveyViewController {

    /**
     * 생애주기 설문 화면
     */
    @GetMapping("/lifecycle/survey")
    public String lifecycleSurvey(Model model) {

        model.addAttribute("currentHousingTypes", CurrentHousingType.values());
        model.addAttribute("industryCodes", IndustryCode.values());
        model.addAttribute("salaryGrowthScenarios", SalaryGrowthScenario.values());
        model.addAttribute("lifestyleLevels", LifestyleLevel.values());
        model.addAttribute("housingTypes", HousingType.values());
        model.addAttribute("vehicleConditions", VehicleCondition.values());
        model.addAttribute("vehicleClasses", VehicleClass.values());
        model.addAttribute("repaymentTypes", LifecycleRepaymentType.values());
        model.addAttribute("repaymentActions", RepaymentAction.values());

        // templates/lifecycle/lifecycle-survey.html 반환
        return "lifecycle/lifecycle-survey";
    }
}
