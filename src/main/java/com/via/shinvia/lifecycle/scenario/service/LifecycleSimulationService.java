package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleBaseStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResultDto;
import com.via.shinvia.lifecycle.survey.dto.LifecycleBaseSurveyResponse;
import com.via.shinvia.lifecycle.survey.service.LifecycleSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LifecycleSimulationService {

    private final LifecycleSurveyService lifecycleSurveyService;
    private final LifecycleProjectionService lifecycleProjectionService;
    private final LifecycleEventInputAssemblerService lifecycleEventInputAssemblerService;
    private final LifecycleEventSequenceService lifecycleEventSequenceService;
    private final LifecycleScenarioResultMapperService lifecycleScenarioResultMapperService;
    private final com.via.shinvia.lifecycle.scenario.mapper.LifecycleScenarioMapper lifecycleScenarioMapper;
    private final com.via.shinvia.finprofile.FinancialProfileMapper financialProfileMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = createObjectMapper();

    private static com.fasterxml.jackson.databind.ObjectMapper createObjectMapper() {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public LifecycleScenarioResultDto simulate(
            Long userId,
            String loginEmail,
            Long scenarioId,
            LifecycleBaseStateDto baseState
    ) {
        LifecycleBaseSurveyResponse baseSurvey =
                lifecycleSurveyService.getBaseSurvey(userId);

        LifecycleBaseStateDto mergedBaseState =
                mergeBaseSurvey(userId, baseState, baseSurvey);

        LifecycleFinancialStateDto initialState =
                lifecycleProjectionService.createInitialState(mergedBaseState);

        List<LifecycleEventInput> inputs =
                lifecycleEventInputAssemblerService.assembleScenario(
                        userId,
                        loginEmail,
                        scenarioId
                );

        List<LifecycleEventResult> eventResults =
                lifecycleEventSequenceService.execute(
                        initialState,
                        inputs,
                        mergedBaseState.getAnnualSalaryGrowthRate(),
                        null
                );

        LifecycleScenarioResultDto result = lifecycleScenarioResultMapperService.toScenarioResult(
                scenarioId,
                userId,
                initialState,
                inputs,
                eventResults
        );

        // 8. 시뮬레이션 결과 영속화 (DB에 result_data 컬럼이 존재할 때만 안전하게 저장)
        saveResultSafely(scenarioId, userId, result);

        return result;
    }

    private void saveResultSafely(Long scenarioId, Long userId, LifecycleScenarioResultDto result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            lifecycleScenarioMapper.updateSimulationResult(scenarioId, userId, resultJson);
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger(LifecycleSimulationService.class)
                    .warn("[LifecycleSimulationService] Failed to persist simulation result (table may need result_data column): {}", t.getMessage());
        }
    }

    public LifecycleScenarioResultDto getSimulationResult(Long userId, Long scenarioId) {
        try {
            String json = lifecycleScenarioMapper.findSimulationResult(scenarioId, userId);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, LifecycleScenarioResultDto.class);
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger(LifecycleSimulationService.class)
                    .warn("[LifecycleSimulationService] Failed to read simulation result: {}", t.getMessage());
            return null;
        }
    }

    private LifecycleBaseStateDto mergeBaseSurvey(
            Long userId,
            LifecycleBaseStateDto baseState,
            LifecycleBaseSurveyResponse baseSurvey
    ) {
        LifecycleBaseStateDto merged = baseState != null
                ? baseState
                : new LifecycleBaseStateDto();

        merged.setUserId(userId);

        if (merged.getBaseDate() == null) {
            merged.setBaseDate(LocalDate.now());
        }

        if (userId != null) {
            try {
                com.via.shinvia.finprofile.FinancialProfile profile =
                        financialProfileMapper.findFinancialProfileByUserId(userId);
                if (profile != null) {
                    if (merged.getAnnualIncome() == null && profile.getAnnualIncome() != null) {
                        merged.setAnnualIncome(profile.getAnnualIncome());
                    }
                    if (merged.getLiquidAssetAmount() == null && profile.getLiquidAssetAmount() != null) {
                        merged.setLiquidAssetAmount(profile.getLiquidAssetAmount());
                    }
                }
            } catch (Exception e) {
                // Fallback gracefully if financial profile query fails
            }
        }

        if (merged.getAnnualIncome() == null) {
            merged.setAnnualIncome(new java.math.BigDecimal("40000000"));
        }

        if (merged.getLiquidAssetAmount() == null) {
            merged.setLiquidAssetAmount(new java.math.BigDecimal("15000000"));
        }

        if (baseSurvey != null) {
            if (merged.getMonthlyLivingExpense() == null) {
                merged.setMonthlyLivingExpense(baseSurvey.getMonthlyLivingExpense());
            }

            if (merged.getCurrentHousingType() == null) {
                merged.setCurrentHousingType(baseSurvey.getCurrentHousingType());
            }

            if (merged.getMonthlyHousingExpense() == null) {
                merged.setMonthlyHousingExpense(baseSurvey.getMonthlyHousingExpense());
            }

            if (merged.getIndustryCode() == null) {
                merged.setIndustryCode(baseSurvey.getIndustryCode());
            }

            if (merged.getSalaryGrowthScenario() == null) {
                merged.setSalaryGrowthScenario(baseSurvey.getSalaryGrowthScenario());
            }

            if (merged.getAnnualSalaryGrowthRate() == null) {
                merged.setAnnualSalaryGrowthRate(baseSurvey.getCustomSalaryGrowthRate());
            }
        }

        if (merged.getAnnualSalaryGrowthRate() == null) {
            merged.setAnnualSalaryGrowthRate(new java.math.BigDecimal("0.03"));
        }
        if (merged.getMonthlyLivingExpense() == null) {
            merged.setMonthlyLivingExpense(new java.math.BigDecimal("1500000"));
        }
        if (merged.getMonthlyHousingExpense() == null) {
            merged.setMonthlyHousingExpense(java.math.BigDecimal.ZERO);
        }
        if (merged.getCurrentHousingType() == null) {
            merged.setCurrentHousingType("FAMILY");
        }

        return merged;
    }
}
