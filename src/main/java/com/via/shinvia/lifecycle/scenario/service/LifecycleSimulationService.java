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

    @Transactional
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

        return lifecycleScenarioResultMapperService.toScenarioResult(
                scenarioId,
                userId,
                initialState,
                inputs,
                eventResults
        );
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

        if (baseSurvey == null) {
            return merged;
        }

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

        return merged;
    }
}
