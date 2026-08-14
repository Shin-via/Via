package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.scenario.mapper.LifecycleScenarioMapper;
import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioRecord;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioCreateRequest;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResponse;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioUpdateRequest;
import com.via.shinvia.lifecycle.survey.dto.LifecycleBaseSurveyResponse;
import com.via.shinvia.lifecycle.survey.service.LifecycleSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LifecycleScenarioService {

    private static final Set<String> EDITABLE_STATUSES = Set.of(
            "DRAFT", "ACTIVE", "COMPLETED"
    );

    private final LifecycleScenarioMapper lifecycleScenarioMapper;
    private final LifecycleSurveyService lifecycleSurveyService;

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

    @Transactional(readOnly = true)
    public List<LifecycleScenarioResponse> getScenarios(Long userId) {
        return lifecycleScenarioMapper.findScenariosByUserId(userId);
    }

    @Transactional(readOnly = true)
    public LifecycleScenarioResponse getScenario(Long userId, Long scenarioId) {
        LifecycleScenarioResponse scenario =
                lifecycleScenarioMapper.findScenarioByIdAndUserId(
                        scenarioId,
                        userId
                );
        if (scenario == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "시나리오를 찾을 수 없습니다."
            );
        }
        return scenario;
    }

    @Transactional
    public LifecycleScenarioResponse createScenario(
            Long userId,
            LifecycleScenarioCreateRequest request
    ) {
        requireCompleteBaseSurvey(userId);
        String scenarioName = requireScenarioName(request.getScenarioName());

        LifecycleScenarioRecord scenario = new LifecycleScenarioRecord();
        scenario.setUserId(userId);
        scenario.setScenarioName(scenarioName);
        scenario.setDescription(normalizeDescription(request.getDescription()));
        scenario.setBaseDate(LocalDate.now());
        scenario.setStatus("DRAFT");
        lifecycleScenarioMapper.insertScenario(scenario);

        return getScenario(userId, scenario.getLifecycleScenarioId());
    }

    @Transactional
    public LifecycleScenarioResponse updateScenario(
            Long userId,
            Long scenarioId,
            LifecycleScenarioUpdateRequest request
    ) {
        LifecycleScenarioResponse current = getScenario(userId, scenarioId);
        String scenarioName = request.getScenarioName() == null
                ? current.getScenarioName()
                : requireScenarioName(request.getScenarioName());
        String description = request.getDescription() == null
                ? current.getDescription()
                : normalizeDescription(request.getDescription());
        String status = request.getStatus() == null
                ? current.getStatus()
                : normalizeStatus(request.getStatus());

        lifecycleScenarioMapper.updateScenario(
                scenarioId,
                userId,
                scenarioName,
                description,
                status
        );
        return getScenario(userId, scenarioId);
    }

    @Transactional
    public void archiveScenario(Long userId, Long scenarioId) {
        int updated = lifecycleScenarioMapper.archiveScenario(
                scenarioId,
                userId
        );
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "시나리오를 찾을 수 없습니다."
            );
        }
    }

    private String requireScenarioName(String scenarioName) {
        if (scenarioName == null || scenarioName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "시나리오 이름을 입력해주세요."
            );
        }
        String normalized = scenarioName.trim();
        if (normalized.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "시나리오 이름은 100자 이하로 입력해주세요."
            );
        }
        return normalized;
    }

    private void requireCompleteBaseSurvey(Long userId) {
        LifecycleBaseSurveyResponse baseSurvey =
                lifecycleSurveyService.getBaseSurvey(userId);
        boolean complete = baseSurvey != null
                && baseSurvey.getMonthlyLivingExpense() != null
                && baseSurvey.getCurrentHousingType() != null
                && baseSurvey.getMonthlyHousingExpense() != null
                && baseSurvey.getIndustryCode() != null
                && baseSurvey.getSalaryGrowthScenario() != null
                && (!"CUSTOM".equals(baseSurvey.getSalaryGrowthScenario())
                    || baseSurvey.getCustomSalaryGrowthRate() != null);

        if (!complete) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "기본 생활정보를 먼저 완료해주세요."
            );
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!EDITABLE_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 시나리오 상태입니다."
            );
        }
        return normalized;
    }
}
