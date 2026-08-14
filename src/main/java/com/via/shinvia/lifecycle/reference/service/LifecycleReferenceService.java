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
            throw new IllegalStateException(
                    "조건에 맞는 생애주기 기준값을 찾을 수 없습니다."
                            + " eventType=" + eventType
                            + ", referenceType=" + referenceType
                            + ", lifestyleLevel=" + lifestyleLevel
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

        if (reference.getAmountValue() == null) {
            throw new IllegalStateException(
                    "조회한 기준자료에 금액 값이 없습니다."
                            + " referenceType=" + referenceType
            );
        }

        return reference.getAmountValue();
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

        if (reference.getRateValue() == null) {
            throw new IllegalStateException(
                    "조회한 기준자료에 비율 값이 없습니다."
                            + " referenceType=" + referenceType
            );
        }

        return reference.getRateValue();
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

        if (reference.getNumericValue() == null) {
            throw new IllegalStateException(
                    "조회한 기준자료에 숫자 값이 없습니다."
                            + " referenceType=" + referenceType
            );
        }

        return reference.getNumericValue();
    }
}