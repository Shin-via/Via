package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.common.model.SupportEffectType;
import com.via.shinvia.lifecycle.recommendation.service.LifecycleProductService;
import com.via.shinvia.lifecycle.recommendation.service.LifecycleWelfareService;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import com.via.shinvia.lifecycle.survey.dto.*;
import com.via.shinvia.lifecycle.survey.service.LifecycleSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LifecycleEventInputAssemblerService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private static final String LIFESTYLE_COST_MULTIPLIER =
            "LIFESTYLE_COST_MULTIPLIER";

    private static final String TOTAL_COST = "TOTAL_COST";
    private static final String POSTPARTUM_CARE_CENTER_COST =
            "POSTPARTUM_CARE_CENTER_COST";
    private static final String POSTPARTUM_HOME_COST =
            "POSTPARTUM_HOME_COST";
    private static final String MONTHLY_CHILDCARE_COST =
            "MONTHLY_CHILDCARE_COST";

    private static final String VEHICLE_BASE_PRICE =
            "VEHICLE_BASE_PRICE";
    private static final String VEHICLE_MONTHLY_MAINTENANCE_COST =
            "VEHICLE_MONTHLY_MAINTENANCE_COST";

    private static final String RENT_BASE_DEPOSIT =
            "RENT_BASE_DEPOSIT";
    private static final String MONTHLY_RENT_BASE_AMOUNT =
            "MONTHLY_RENT_BASE_AMOUNT";

    private static final String JEONSE_BASE_DEPOSIT =
            "JEONSE_BASE_DEPOSIT";

    private static final String HOME_BASE_PURCHASE_PRICE =
            "HOME_BASE_PURCHASE_PRICE";
    private static final String ACQUISITION_TAX_RATE =
            "ACQUISITION_TAX_RATE";

    private static final String BASE_AREA_SQM = "BASE_AREA_SQM";

    private final LifecycleSurveyService surveyService;
    private final LifecycleReferenceService referenceService;
    private final LifecycleProductService productService;
    private final LifecycleWelfareService welfareService;

    public List<LifecycleEventInput> assembleScenario(
            Long userId,
            String loginEmail,
            Long scenarioId
    ) {
        return surveyService.getTimelineEvents(scenarioId)
                .stream()
                .map(event -> assembleEvent(
                        userId,
                        loginEmail,
                        event.getEventType(),
                        event.getEventId()
                ))
                .toList();
    }

    public LifecycleEventInput assembleEvent(
            Long userId,
            String loginEmail,
            LifecycleEventType eventType,
            Long lifecycleEventId
    ) {
        return switch (eventType) {
            case MARRIAGE ->
                    assembleMarriage(userId, loginEmail, lifecycleEventId);
            case CHILDBIRTH ->
                    assembleChildbirth(userId, loginEmail, lifecycleEventId);
            case VEHICLE_PURCHASE ->
                    assembleVehicle(userId, loginEmail, lifecycleEventId);
            case MONTHLY_RENT ->
                    assembleMonthlyRent(userId, loginEmail, lifecycleEventId);
            case JEONSE ->
                    assembleJeonse(userId, loginEmail, lifecycleEventId);
            case HOME_PURCHASE ->
                    assembleHomePurchase(userId, loginEmail, lifecycleEventId);
            case REPAYMENT ->
                    assembleRepayment(userId, loginEmail, lifecycleEventId);
        };
    }

    private LifecycleEventInput assembleMarriage(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        MarriageSurveyResponse survey =
                surveyService.getMarriageSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.MARRIAGE,
                        survey.getRegionSido(),
                        survey.getRegionSigungu()
                );

        BigDecimal estimatedCost = positiveOrDefault(
                survey.getCustomEstimatedCost(),
                referenceAmount(LifecycleEventType.MARRIAGE, TOTAL_COST)
                        .multiply(lifestyleMultiplier(
                                LifecycleEventType.MARRIAGE,
                                survey.getLifestyleLevel()
                        ))
        );

        BigDecimal userShare = estimatedCost.multiply(
                defaultIfNull(survey.getUserContributionRate(), ONE)
        );

        BigDecimal familySupport = nvl(survey.getFamilySupportAmount());
        BigDecimal cashInflow = sumSupportAmount(
                supports,
                SupportEffectType.CASH_INFLOW
        );

        BigDecimal userRequiredAmount = maxZero(
                userShare.subtract(familySupport).subtract(cashInflow)
        );

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.MARRIAGE,
                        userRequiredAmount,
                        36
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.MARRIAGE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(estimatedCost))
                .userRequiredAmount(money(userRequiredAmount))
                .userContributionAmount(money(userShare))
                .additionalMonthlyExpense(ZERO)
                .cashInflowAmount(money(cashInflow))
                .familySupportAmount(money(familySupport))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(ZERO)
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleChildbirth(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        ChildbirthSurveyResponse survey =
                surveyService.getChildbirthSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.CHILDBIRTH,
                        survey.getRegionSido(),
                        survey.getRegionSigungu()
                );

        BigDecimal initialCost = Boolean.TRUE.equals(survey.getPostpartumCare())
                ? referenceAmount(LifecycleEventType.CHILDBIRTH, POSTPARTUM_CARE_CENTER_COST)
                : referenceAmount(LifecycleEventType.CHILDBIRTH, POSTPARTUM_HOME_COST);

        initialCost = initialCost.multiply(lifestyleMultiplier(
                LifecycleEventType.CHILDBIRTH,
                survey.getLifestyleLevel()
        ));

        BigDecimal monthlyChildcareCost =
                referenceAmount(LifecycleEventType.CHILDBIRTH, MONTHLY_CHILDCARE_COST)
                        .multiply(lifestyleMultiplier(
                                LifecycleEventType.CHILDBIRTH,
                                survey.getLifestyleLevel()
                        ));

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal monthlySupport =
                sumSupportAmount(supports, SupportEffectType.MONTHLY_CASH_INFLOW);

        BigDecimal additionalMonthlyExpense =
                maxZero(monthlyChildcareCost.subtract(monthlySupport));

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.CHILDBIRTH
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.CHILDBIRTH,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(initialCost))
                .userRequiredAmount(money(maxZero(initialCost.subtract(cashInflow))))
                .additionalMonthlyExpense(money(additionalMonthlyExpense))
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(ZERO)
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleVehicle(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        VehicleSurveyResponse survey =
                surveyService.getVehicleSurvey(lifecycleEventId);

        BigDecimal estimatedPrice = survey.getVehiclePrice() != null
                && survey.getVehiclePrice().signum() > 0
                ? survey.getVehiclePrice()
                : calculateVehiclePrice();

        BigDecimal newLoanAmount = nvl(survey.getLoanAmount());

        if (newLoanAmount.signum() == 0
                && survey.getCashPaymentAmount() != null) {
            newLoanAmount = maxZero(
                    estimatedPrice.subtract(survey.getCashPaymentAmount())
            );
        }

        BigDecimal userRequiredAmount =
                maxZero(estimatedPrice.subtract(newLoanAmount));

        BigDecimal monthlyMaintenance = survey.getMonthlyMaintenanceCost() != null
                && survey.getMonthlyMaintenanceCost().signum() >= 0
                ? survey.getMonthlyMaintenanceCost()
                : referenceAmount(
                        LifecycleEventType.VEHICLE_PURCHASE,
                        VEHICLE_MONTHLY_MAINTENANCE_COST
                );

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.VEHICLE_PURCHASE,
                        positiveOrNull(newLoanAmount),
                        survey.getLoanPeriodMonths()
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.VEHICLE_PURCHASE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                null)
                .estimatedCost(money(estimatedPrice))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(money(monthlyMaintenance))
                .cashInflowAmount(ZERO)
                .newLoanAmount(money(newLoanAmount))
                .acquiredAssetAmount(money(estimatedPrice))
                .supports(List.of())
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleMonthlyRent(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        MonthlyRentSurveyResponse survey =
                surveyService.getMonthlyRentSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.MONTHLY_RENT,
                        survey.getRegionSido(),
                        survey.getRegionSigungu()
                );

        BigDecimal deposit = positiveOrDefault(
                survey.getDesiredDeposit(),
                calculateHousingAmount(
                        LifecycleEventType.MONTHLY_RENT,
                        RENT_BASE_DEPOSIT,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                )
        );

        BigDecimal monthlyRent = positiveOrDefault(
                survey.getDesiredMonthlyRent(),
                calculateHousingAmount(
                        LifecycleEventType.MONTHLY_RENT,
                        MONTHLY_RENT_BASE_AMOUNT,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                )
        );

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal monthlySupport =
                sumSupportAmount(supports, SupportEffectType.MONTHLY_CASH_INFLOW);

        BigDecimal monthlyExpense = monthlyRent
                .add(nvl(survey.getMonthlyManagementFee()))
                .subtract(monthlySupport);

        BigDecimal userRequiredAmount =
                maxZero(deposit.subtract(cashInflow));

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.MONTHLY_RENT,
                        positiveOrNull(userRequiredAmount),
                        36
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.MONTHLY_RENT,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(deposit))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(money(maxZero(monthlyExpense)))
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(money(deposit))
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleJeonse(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        JeonseSurveyResponse survey =
                surveyService.getJeonseSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.JEONSE,
                        survey.getRegionSido(),
                        survey.getRegionSigungu()
                );

        BigDecimal deposit = positiveOrDefault(
                survey.getDesiredJeonseAmount(),
                calculateHousingAmount(
                        LifecycleEventType.JEONSE,
                        JEONSE_BASE_DEPOSIT,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                )
        );

        BigDecimal newLoanAmount = nvl(survey.getDesiredLoanAmount());

        if (newLoanAmount.signum() == 0
                && survey.getOwnFundAmount() != null) {
            newLoanAmount = maxZero(
                    deposit.subtract(survey.getOwnFundAmount())
            );
        }

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal userRequiredAmount =
                maxZero(deposit.subtract(newLoanAmount).subtract(cashInflow));

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.JEONSE,
                        positiveOrNull(newLoanAmount),
                        24
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.JEONSE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(deposit))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(ZERO)
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(money(newLoanAmount))
                .acquiredAssetAmount(money(deposit))
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleHomePurchase(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        HomePurchaseSurveyResponse survey =
                surveyService.getHomePurchaseSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.HOME_PURCHASE,
                        survey.getRegionSido(),
                        survey.getRegionSigungu()
                );

        BigDecimal purchasePrice = positiveOrDefault(
                survey.getDesiredPurchasePrice(),
                calculateHousingAmount(
                        LifecycleEventType.HOME_PURCHASE,
                        HOME_BASE_PURCHASE_PRICE,
                        survey.getLifestyleLevel(),
                        survey.getHousingType(),
                        survey.getDesiredArea()
                )
        );

        BigDecimal acquisitionTax = purchasePrice.multiply(
                referenceRate(
                        LifecycleEventType.HOME_PURCHASE,
                        ACQUISITION_TAX_RATE
                )
        );

        BigDecimal totalCost = purchasePrice.add(acquisitionTax);

        BigDecimal newLoanAmount = survey.getOwnFundAmount() == null
                ? ZERO
                : maxZero(purchasePrice.subtract(survey.getOwnFundAmount()));

        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal userRequiredAmount =
                maxZero(totalCost.subtract(newLoanAmount).subtract(cashInflow));

        BigDecimal recommendationAmount = newLoanAmount.signum() > 0
                ? newLoanAmount
                : purchasePrice;

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.HOME_PURCHASE,
                        positiveOrNull(recommendationAmount),
                        survey.getLoanPeriodMonths()
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.HOME_PURCHASE,
                survey.getEventOrder(),
                survey.getTargetDate(),
                survey.getLifestyleLevel())
                .estimatedCost(money(totalCost))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(ZERO)
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(money(newLoanAmount))
                .acquiredAssetAmount(money(purchasePrice))
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private LifecycleEventInput assembleRepayment(
            Long userId,
            String loginEmail,
            Long lifecycleEventId
    ) {
        RepaymentSurveyResponse survey =
                surveyService.getRepaymentSurvey(lifecycleEventId);

        List<LifecycleSupportDto> supports =
                welfareService.getSupports(
                        LifecycleEventType.REPAYMENT,
                        null,
                        null
                );

        BigDecimal repaymentAmount = nvl(survey.getRepaymentAmount());
        BigDecimal cashInflow =
                sumSupportAmount(supports, SupportEffectType.CASH_INFLOW);

        BigDecimal userRequiredAmount =
                maxZero(repaymentAmount.subtract(cashInflow));

        List<LifecycleProductDto> products =
                productService.getRecommendedProducts(
                        userId,
                        loginEmail,
                        LifecycleEventType.REPAYMENT,
                        positiveOrNull(repaymentAmount),
                        60
                );

        return baseBuilder(survey.getLifecycleEventId(),
                LifecycleEventType.REPAYMENT,
                survey.getEventOrder(),
                survey.getTargetDate(),
                null)
                .estimatedCost(money(repaymentAmount))
                .userRequiredAmount(money(userRequiredAmount))
                .additionalMonthlyExpense(money(nvl(
                        survey.getAdditionalMonthlyRepayment()
                )))
                .cashInflowAmount(money(cashInflow))
                .newLoanAmount(ZERO)
                .acquiredAssetAmount(ZERO)
                .supports(supports)
                .recommendedProducts(products)
                .build();
    }

    private BigDecimal calculateVehiclePrice() {
        return referenceAmount(
                LifecycleEventType.VEHICLE_PURCHASE,
                VEHICLE_BASE_PRICE
        );
    }

    private BigDecimal calculateHousingAmount(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel,
            String housingType,
            BigDecimal desiredArea
    ) {
        BigDecimal amount = referenceAmount(eventType, referenceType)
                .multiply(lifestyleMultiplier(eventType, lifestyleLevel))
                .multiply(housingTypeMultiplier(eventType, housingType));

        BigDecimal baseArea = referenceNumeric(eventType, BASE_AREA_SQM);

        if (desiredArea != null
                && desiredArea.signum() > 0
                && baseArea.signum() > 0) {
            amount = amount.multiply(
                    desiredArea.divide(baseArea, 6, RoundingMode.HALF_UP)
            );
        }

        return amount;
    }

    private LifecycleEventInput.LifecycleEventInputBuilder baseBuilder(
            Long lifecycleEventId,
            LifecycleEventType eventType,
            Integer eventOrder,
            java.time.LocalDate targetDate,
            LifestyleLevel lifestyleLevel
    ) {
        return LifecycleEventInput.builder()
                .lifecycleEventId(lifecycleEventId)
                .eventType(eventType)
                .eventOrder(eventOrder)
                .targetDate(targetDate)
                .lifestyleLevel(lifestyleLevel);
    }

    private BigDecimal lifestyleMultiplier(
            LifecycleEventType eventType,
            LifestyleLevel lifestyleLevel
    ) {
        if (lifestyleLevel == null
                || lifestyleLevel == LifestyleLevel.CUSTOM) {
            return ONE;
        }

        return referenceService.getNationalRate(
                eventType,
                LIFESTYLE_COST_MULTIPLIER,
                lifestyleLevel
        );
    }

    private BigDecimal housingTypeMultiplier(
            LifecycleEventType eventType,
            String housingType
    ) {
        if (housingType == null || housingType.isBlank()) {
            return ONE;
        }

        return referenceRate(
                eventType,
                "HOUSING_TYPE_MULTIPLIER_" + housingType
        );
    }

    private BigDecimal referenceAmount(
            LifecycleEventType eventType,
            String referenceType
    ) {
        return referenceService.getNationalAmount(
                eventType,
                referenceType,
                null
        );
    }

    private BigDecimal referenceRate(
            LifecycleEventType eventType,
            String referenceType
    ) {
        return referenceService.getNationalRate(
                eventType,
                referenceType,
                null
        );
    }

    private BigDecimal referenceNumeric(
            LifecycleEventType eventType,
            String referenceType
    ) {
        return referenceService.getNationalNumeric(
                eventType,
                referenceType,
                null
        );
    }

    private BigDecimal sumSupportAmount(
            List<LifecycleSupportDto> supports,
            SupportEffectType effectType
    ) {
        if (supports == null || supports.isEmpty()) {
            return ZERO;
        }

        return supports.stream()
                .filter(support -> "ELIGIBLE".equals(
                        support.getRecommendationStatus()
                ))
                .filter(support -> support.getEffectType() == effectType)
                .map(LifecycleSupportDto::getAmount)
                .filter(amount -> amount != null)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal positiveOrDefault(
            BigDecimal value,
            BigDecimal defaultValue
    ) {
        if (value != null && value.signum() > 0) {
            return value;
        }

        return defaultValue;
    }

    private BigDecimal positiveOrNull(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }

        return value;
    }

    private BigDecimal defaultIfNull(
            BigDecimal value,
            BigDecimal defaultValue
    ) {
        return value == null ? defaultValue : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal maxZero(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return ZERO;
        }

        return value;
    }

    private BigDecimal money(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
    }
}
