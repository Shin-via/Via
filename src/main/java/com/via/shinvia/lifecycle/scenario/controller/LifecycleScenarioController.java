package com.via.shinvia.lifecycle.scenario.controller;

import com.via.shinvia.lifecycle.scenario.service.LifecycleScenarioService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lifecycle/scenarios")
public class LifecycleScenarioController {

    private final LifecycleScenarioService lifecycleScenarioService;
    private final CurrentUser currentUser;

    @PostMapping("/current")
    public ResponseEntity<Map<String, Long>> getOrCreateCurrent(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        Long scenarioId =
                lifecycleScenarioService.getOrCreateActiveScenario(userId);
        return ResponseEntity.ok(Map.of("scenarioId", scenarioId));
    }
}
