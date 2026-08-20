package com.via.shinvia.lifecycle.reference.service;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.reference.dto.LifecycleReferenceDto;
import com.via.shinvia.lifecycle.reference.mapper.LifecycleReferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LifecycleReferenceService {

    private final LifecycleReferenceMapper lifecycleReferenceMapper;

    /**
     * 조건에 맞는 최신 기준값을 조회한다.
     *
     * DTO 전체를 반환하므로 금액뿐 아니라
     * sourceName, sourceTitle, sourceUrl도 사용할 수 있다.
     */
    public LifecycleReferenceDto getLatestReference(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel,
            String regionSido,
            String regionSigungu
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException(
                    "생애주기 이벤트 유형은 필수입니다."
            );
        }

        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException(
                    "기준값 유형은 필수입니다."
            );
        }

        LifecycleReferenceDto reference =
                lifecycleReferenceMapper.findLatestReference(
                        eventType,
                        referenceType,
                        lifestyleLevel,
                        regionSido,
                        regionSigungu
                );

        if (reference == null) {
            reference = buildDefaultReference(
                    eventType,
                    referenceType,
                    lifestyleLevel,
                    regionSido,
                    regionSigungu
            );
        }

        return reference;
    }

    /**
     * 전국 기준자료 조회
     */
    public LifecycleReferenceDto getNationalReference(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        return getLatestReference(
                eventType,
                referenceType,
                lifestyleLevel,
                null,
                null
        );
    }

    /**
     * 지역별 금액형 기준값 조회 (지역 데이터가 없으면 전국 기본값 조회)
     */
    public BigDecimal getRegionalAmount(
            LifecycleEventType eventType,
            String referenceType,
            String regionSido,
            String regionSigungu,
            LifestyleLevel lifestyleLevel
    ) {
        LifecycleReferenceDto reference = getLatestReference(
                eventType,
                referenceType,
                lifestyleLevel,
                regionSido,
                regionSigungu
        );

        if (reference != null && reference.getAmountValue() != null) {
            return reference.getAmountValue();
        }

        return getNationalAmount(eventType, referenceType, lifestyleLevel);
    }

    /**
     * 전국 금액형 기준값 조회
     */
    public BigDecimal getNationalAmount(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        LifecycleReferenceDto reference =
                getNationalReference(
                        eventType,
                        referenceType,
                        lifestyleLevel
                );

        if (reference != null && reference.getAmountValue() != null) {
            return reference.getAmountValue();
        }

        return resolveDefaultAmount(eventType, referenceType);
    }

    /**
     * 전국 비율형 기준값 조회
     */
    public BigDecimal getNationalRate(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        LifecycleReferenceDto reference =
                getNationalReference(
                        eventType,
                        referenceType,
                        lifestyleLevel
                );

        if (reference.getRateValue() != null) {
            return reference.getRateValue();
        }

        return resolveDefaultRate(eventType, referenceType, lifestyleLevel);
    }

    /**
     * 전국 일반 숫자형 기준값 조회
     *
     * 예: 기준 전용면적 59㎡
     */
    public BigDecimal getNationalNumeric(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        LifecycleReferenceDto reference =
                getNationalReference(
                        eventType,
                        referenceType,
                        lifestyleLevel
                );

        if (reference.getNumericValue() != null) {
            return reference.getNumericValue();
        }

        return resolveDefaultNumeric(eventType, referenceType);
    }

    private LifecycleReferenceDto buildDefaultReference(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel,
            String regionSido,
            String regionSigungu
    ) {
        return LifecycleReferenceDto.builder()
                .eventType(eventType)
                .referenceType(referenceType)
                .lifestyleLevel(lifestyleLevel)
                .regionSido(regionSido)
                .regionSigungu(regionSigungu)
                .amountValue(resolveDefaultAmount(eventType, referenceType))
                .rateValue(resolveDefaultRate(eventType, referenceType, lifestyleLevel))
                .numericValue(resolveDefaultNumeric(eventType, referenceType))
                .referenceYear(2026)
                .sourceName("통계청 / 국토교통부 / 한국소비자원")
                .sourceTitle("생애주기 표준 기준데이터")
                .active(true)
                .build();
    }

    private BigDecimal resolveDefaultAmount(LifecycleEventType eventType, String referenceType) {
        if (referenceType == null) return BigDecimal.ZERO;
        return switch (referenceType) {
            case "TOTAL_COST", "MARRIAGE_TOTAL_COST" -> new BigDecimal("21390000.00");
            case "MEAL_COST" -> new BigDecimal("65000.00");
            case "WEDDING_HALL_PACKAGE_COST" -> new BigDecimal("10000000.00");
            case "FURNITURE_COST" -> new BigDecimal("12000000.00");
            case "HONEYMOON_COST" -> new BigDecimal("6000000.00");
            case "POSTPARTUM_CARE_CENTER_COST" -> new BigDecimal("2865000.00");
            case "POSTPARTUM_HOME_COST" -> new BigDecimal("1500000.00");
            case "MONTHLY_CHILDCARE_COST" -> new BigDecimal("800000.00");
            case "VEHICLE_BASE_PRICE" -> new BigDecimal("36000000.00");
            case "VEHICLE_MONTHLY_MAINTENANCE_COST" -> new BigDecimal("400000.00");
            case "RENT_BASE_DEPOSIT" -> new BigDecimal("20000000.00");
            case "MONTHLY_RENT_BASE_AMOUNT" -> new BigDecimal("700000.00");
            case "JEONSE_BASE_DEPOSIT" -> new BigDecimal("300000000.00");
            case "HOME_BASE_PURCHASE_PRICE" -> new BigDecimal("600000000.00");
            default -> new BigDecimal("10000000.00");
        };
    }

    private BigDecimal resolveDefaultRate(
            LifecycleEventType eventType,
            String referenceType,
            LifestyleLevel lifestyleLevel
    ) {
        if (referenceType == null) return BigDecimal.ONE;

        if (referenceType.contains("LIFESTYLE_COST_MULTIPLIER")) {
            if (lifestyleLevel == null) return BigDecimal.ONE;
            return switch (lifestyleLevel) {
                case PRACTICAL -> new BigDecimal("0.800000");
                case AVERAGE -> new BigDecimal("1.000000");
                case RELAXED -> new BigDecimal("1.300000");
                case PREMIUM -> new BigDecimal("1.800000");
                default -> BigDecimal.ONE;
            };
        }

        if (referenceType.startsWith("HOUSING_TYPE_MULTIPLIER_")) {
            String type = referenceType.substring("HOUSING_TYPE_MULTIPLIER_".length()).toUpperCase();
            return switch (type) {
                case "APARTMENT" -> new BigDecimal("1.150000");
                case "VILLA" -> new BigDecimal("0.850000");
                case "OFFICETEL" -> new BigDecimal("1.000000");
                default -> BigDecimal.ONE;
            };
        }

        if (referenceType.startsWith("VEHICLE_CLASS_MULTIPLIER_")) {
            String type = referenceType.substring("VEHICLE_CLASS_MULTIPLIER_".length()).toUpperCase();
            return switch (type) {
                case "COMPACT" -> new BigDecimal("0.600000");
                case "SEDAN" -> new BigDecimal("1.000000");
                case "SUV" -> new BigDecimal("1.150000");
                case "LUXURY" -> new BigDecimal("1.500000");
                default -> BigDecimal.ONE;
            };
        }

        if (referenceType.startsWith("VEHICLE_CONDITION_MULTIPLIER_")) {
            String type = referenceType.substring("VEHICLE_CONDITION_MULTIPLIER_".length()).toUpperCase();
            return "USED".equals(type) ? new BigDecimal("0.600000") : new BigDecimal("1.000000");
        }

        return switch (referenceType) {
            case "ACQUISITION_TAX_RATE" -> new BigDecimal("0.011000");
            case "PREPAYMENT_FEE_RATE" -> new BigDecimal("0.006500");
            default -> BigDecimal.ONE;
        };
    }

    private BigDecimal resolveDefaultNumeric(LifecycleEventType eventType, String referenceType) {
        if ("BASE_AREA_SQM".equals(referenceType)) {
            return eventType == LifecycleEventType.HOME_PURCHASE
                    ? new BigDecimal("84.0000")
                    : new BigDecimal("59.0000");
        }
        return new BigDecimal("59.0000");
    }
}