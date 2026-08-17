package com.via.shinvia.lifecycle.scenario.controller;

import com.via.shinvia.lifecycle.common.dto.LifecycleBaseStateDto;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResultDto;
import com.via.shinvia.lifecycle.scenario.service.LifecycleSimulationService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lifecycle/scenarios")
public class LifecycleSimulationController {

    private final LifecycleSimulationService lifecycleSimulationService;
    private final CurrentUser currentUser;

    @PostMapping("/{scenarioId}/simulate")
    public ResponseEntity<LifecycleScenarioResultDto> simulate(
            Authentication authentication,
            @PathVariable Long scenarioId,
            @RequestBody(required = false) LifecycleBaseStateDto baseState
    ) {
        Long userId = currentUser.getUserId(authentication);
        String loginEmail = authentication != null
                ? authentication.getName()
                : null;

        LifecycleScenarioResultDto result =
                lifecycleSimulationService.simulate(
                        userId,
                        loginEmail,
                        scenarioId,
                        baseState
                );

        return ResponseEntity.ok(result);
    }
}