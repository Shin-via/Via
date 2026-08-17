package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.recommendation.adapter.LoanRecommendationAdapter;
import com.via.shinvia.lifecycle.recommendation.adapter.PolicyRecommendationAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LifecycleProductService {

    private static final int EACH_SOURCE_LIMIT = 3;
    private static final int TOTAL_LIMIT = 5;

    private final LoanRecommendationAdapter loanRecommendationAdapter;
    private final PolicyRecommendationAdapter policyRecommendationAdapter;

    public List<LifecycleProductDto> getRecommendedProducts(
            Long userId,
            String loginEmail,
            LifecycleEventType eventType,
            BigDecimal requestedAmount,
            Integer termMonths
    ) {
        List<LifecycleProductDto> products = new ArrayList<>();

        products.addAll(loanRecommendationAdapter.recommend(
                loginEmail,
                eventType,
                requestedAmount,
                termMonths,
                EACH_SOURCE_LIMIT
        ));
        products.addAll(policyRecommendationAdapter.recommend(
                userId,
                eventType,
                EACH_SOURCE_LIMIT
        ));

        return deduplicate(products).stream()
                .filter(product -> isPurposeRelevant(product, eventType))
                .limit(TOTAL_LIMIT)
                .toList();
    }

    public List<LifecycleProductDto> getRecommendedProducts(
            Long userId,
            String loginEmail,
            LifecycleEventType eventType
    ) {
        return getRecommendedProducts(
                userId,
                loginEmail,
                eventType,
                null,
                null
        );
    }

    private List<LifecycleProductDto> deduplicate(
            List<LifecycleProductDto> products
    ) {
        Map<String, LifecycleProductDto> result = new LinkedHashMap<>();

        for (LifecycleProductDto product : products) {
            if (product == null) {
                continue;
            }
            String key = product.getProductType()
                    + ":"
                    + product.getProductId();
            result.putIfAbsent(key, product);
        }

        return new ArrayList<>(result.values());
    }

    private boolean isPurposeRelevant(
            LifecycleProductDto product,
            LifecycleEventType eventType
    ) {
        String name = product.getProductName();
        if (name == null || name.isBlank()) {
            return false;
        }

        String normalizedName = name.replaceAll("\\s+", "");
        List<String> purposeKeywords = switch (eventType) {
            case MARRIAGE -> List.of("결혼", "혼례", "웨딩");
            case MONTHLY_RENT -> List.of("월세", "임차료", "전월세");
            case JEONSE -> List.of("전세", "임차보증금", "전월세보증금");
            case HOME_PURCHASE -> List.of(
                    "주택", "내집", "구입자금", "디딤돌",
                    "모기지", "보금자리"
            );
            default -> List.of();
        };

        return purposeKeywords.isEmpty()
                || purposeKeywords.stream()
                .anyMatch(normalizedName::contains);
    }
}
