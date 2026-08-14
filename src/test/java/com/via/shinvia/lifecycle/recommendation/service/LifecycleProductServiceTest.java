package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.recommendation.adapter.LoanRecommendationAdapter;
import com.via.shinvia.lifecycle.recommendation.adapter.PolicyRecommendationAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecycleProductServiceTest {

    @Mock
    private LoanRecommendationAdapter loanRecommendationAdapter;

    @Mock
    private PolicyRecommendationAdapter policyRecommendationAdapter;

    @InjectMocks
    private LifecycleProductService lifecycleProductService;

    @Test
    void getRecommendedProductsReturnsListWithoutException() {
        when(loanRecommendationAdapter.recommend(
                anyString(),
                any(LifecycleEventType.class),
                any(),
                any(),
                anyInt()
        )).thenReturn(List.of());
        when(policyRecommendationAdapter.recommend(
                any(),
                any(LifecycleEventType.class),
                anyInt()
        )).thenReturn(List.of());

        List<LifecycleProductDto> products = assertDoesNotThrow(() ->
                lifecycleProductService.getRecommendedProducts(
                        1L,
                        "user@example.com",
                        LifecycleEventType.JEONSE
                )
        );

        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    void getRecommendedProductsMergesDeduplicatesAndLimitsResults() {
        LifecycleProductDto loan1 = LifecycleProductDto.builder()
                .productId(1L)
                .productType("JEONSE_LOAN")
                .productName("전세대출 A")
                .build();

        LifecycleProductDto duplicatedLoan1 = LifecycleProductDto.builder()
                .productId(1L)
                .productType("JEONSE_LOAN")
                .productName("전세대출 A 중복")
                .build();

        LifecycleProductDto loan2 = LifecycleProductDto.builder()
                .productId(2L)
                .productType("JEONSE_LOAN")
                .productName("전세대출 B")
                .build();

        LifecycleProductDto policy1 = LifecycleProductDto.builder()
                .productId(10L)
                .productType("POLICY_LOAN")
                .productName("정책대출 A")
                .build();

        LifecycleProductDto duplicatedPolicy1 = LifecycleProductDto.builder()
                .productId(10L)
                .productType("POLICY_LOAN")
                .productName("정책대출 A 중복")
                .build();

        LifecycleProductDto policy2 = LifecycleProductDto.builder()
                .productId(11L)
                .productType("POLICY_LOAN")
                .productName("정책대출 B")
                .build();

        LifecycleProductDto policy3 = LifecycleProductDto.builder()
                .productId(12L)
                .productType("POLICY_LOAN")
                .productName("정책대출 C")
                .build();

        LifecycleProductDto policy4 = LifecycleProductDto.builder()
                .productId(13L)
                .productType("POLICY_LOAN")
                .productName("정책대출 D")
                .build();

        when(loanRecommendationAdapter.recommend(
                anyString(),
                any(LifecycleEventType.class),
                any(),
                any(),
                anyInt()
        )).thenReturn(List.of(
                loan1,
                duplicatedLoan1,
                loan2
        ));

        when(policyRecommendationAdapter.recommend(
                any(),
                any(LifecycleEventType.class),
                anyInt()
        )).thenReturn(List.of(
                policy1,
                duplicatedPolicy1,
                policy2,
                policy3,
                policy4
        ));

        List<LifecycleProductDto> products =
                lifecycleProductService.getRecommendedProducts(
                        1L,
                        "user@example.com",
                        LifecycleEventType.JEONSE
                );

        assertNotNull(products);
        assertEquals(5, products.size());

        assertEquals("전세대출 A", products.get(0).getProductName());
        assertEquals("전세대출 B", products.get(1).getProductName());
        assertEquals("정책대출 A", products.get(2).getProductName());
        assertEquals("정책대출 B", products.get(3).getProductName());
        assertEquals("정책대출 C", products.get(4).getProductName());
    }
}
