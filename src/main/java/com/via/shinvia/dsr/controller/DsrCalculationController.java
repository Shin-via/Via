package com.via.shinvia.dsr.controller;

import com.via.shinvia.dsr.dto.request.DsrCalculationRequestDto;
import com.via.shinvia.dsr.dto.result.DsrCalculationResultDto;
import com.via.shinvia.dsr.dto.type.*;
import com.via.shinvia.dsr.service.DsrCalculationService;
import com.via.shinvia.loan.ratesimulation.common.type.RepaymentType;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dsr")
@RequiredArgsConstructor
public class DsrCalculationController {
    private final DsrCalculationService dsrCalculationService;
    private final CurrentUser currentUser;

    @GetMapping
    public String showDsrCalculationFrom(Model model) {
        model.addAttribute("dsrCalculationRequest", new DsrCalculationRequestDto());
        addSelectionOptions(model);
        return "dsr/dsr-calculation";
    }

    @PostMapping
    public String calculateDsr(Authentication authentication,
                               @ModelAttribute("dsrCalculationRequest") DsrCalculationRequestDto request,
                               Model model)  {

        try {
            DsrCalculationResultDto result = dsrCalculationService.calculate(currentUser.getUserIdOrNull(authentication),request);
            model.addAttribute("result", result);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        addSelectionOptions(model);

       return "dsr/dsr-calculation";

    }


    private void addSelectionOptions(Model model) {
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("repaymentTypes", RepaymentType.values());
        model.addAttribute("interestRateTypes", InterestRateType.values());
        model.addAttribute("propertyRegions", PropertyRegion.values());
        model.addAttribute("rentalPropertyRegions", RentalPropertyRegion.values());
        model.addAttribute("housingOwnershipTypes", HousingOwnershipType.values());
    }

}
