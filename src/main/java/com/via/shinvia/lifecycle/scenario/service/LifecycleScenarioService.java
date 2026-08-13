package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.scenario.mapper.LifecycleScenarioMapper;
import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LifecycleScenarioService {

    private final LifecycleScenarioMapper lifecycleScenarioMapper;

    @Transactional
    public Long getOrCreateActiveScenario(Long userId) {
        Long scenarioId =
                lifecycleScenarioMapper.findActiveScenarioIdByUserId(userId);

        if (scenarioId != null) {
            return scenarioId;
        }

        LifecycleScenarioRecord scenario = new LifecycleScenarioRecord();
        scenario.setUserId(userId);
        scenario.setScenarioName("금융 라이프 플랜");
        scenario.setDescription("생활 이벤트 기반 금융 시나리오");
        scenario.setBaseDate(LocalDate.now());
        scenario.setStatus("ACTIVE");
        lifecycleScenarioMapper.insertScenario(scenario);
        return scenario.getLifecycleScenarioId();
    }
}
