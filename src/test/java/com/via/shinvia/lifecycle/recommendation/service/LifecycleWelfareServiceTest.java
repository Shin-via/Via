package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.policy.welfare.repository.WelfareSupportProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecycleWelfareServiceTest {

    @Mock
    private WelfareSupportProductRepository repository;

    @InjectMocks
    private LifecycleWelfareService service;

    @Test
    void ranksCandidatesBeforeApplyingFinalLimit() {
        List<WelfareSupportProduct> candidates = List.of(
                product(1L, "출산 관련 안내", "KINFA", null, null, false),
                product(2L, "전국 출산 지원", "BOKJIRO_NATIONAL", null, null, false),
                product(3L, "서울 출산 지원", "BOKJIRO_LOCAL", "서울특별시", null, false),
                product(4L, "강남구 출산 지원", "BOKJIRO_LOCAL", "서울특별시", "강남구", false),
                product(5L, "강남구 출산 신청 지원", "BOKJIRO_LOCAL", "서울특별시", "강남구", true),
                product(6L, "출산 축하금", "KINFA", null, null, false)
        );
        when(repository.findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구"))
                .thenReturn(candidates);

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.CHILDBIRTH,
                "서울특별시",
                "강남구"
        );

        assertEquals(5, result.size());
        assertEquals("강남구 출산 신청 지원", result.get(0).getSupportName());
        assertEquals("강남구 출산 지원", result.get(1).getSupportName());
        assertEquals("서울 출산 지원", result.get(2).getSupportName());
        assertFalse(result.stream().anyMatch(item -> "출산 관련 안내".equals(item.getSupportName())));
        verify(repository).findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구");
    }

    @Test
    void prefersBokjiroForNationalCrossSourceDuplicate() {
        WelfareSupportProduct kinfa = product(
                1L, "공공산림가꾸기", "KINFA", null, null, false
        );
        WelfareSupportProduct bokjiro = product(
                2L, "공공 산림가꾸기", "BOKJIRO_NATIONAL", null, null, false
        );
        when(repository.findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구"))
                .thenReturn(List.of(kinfa, bokjiro));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.CHILDBIRTH,
                "서울특별시",
                "강남구"
        );

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getWelfareSupportProductId());
    }

    @Test
    void keepsLocalAndKinfaProductsWhenOnlyGenericNameMatches() {
        WelfareSupportProduct kinfa = product(
                1L, "공공근로사업", "KINFA", null, null, false
        );
        kinfa.setInstitutionName("서민금융진흥원");
        WelfareSupportProduct local = product(
                2L, "공공근로사업", "BOKJIRO_LOCAL", "서울특별시", "강남구", false
        );
        local.setInstitutionName("강남구청");
        when(repository.findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구"))
                .thenReturn(List.of(kinfa, local));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.CHILDBIRTH,
                "서울특별시",
                "강남구"
        );

        assertEquals(2, result.size());
    }

    @Test
    void supportsVehiclePurchaseWithEventSpecificKeywords() {
        WelfareSupportProduct vehicle = product(
                1L, "친환경 자동차 구입비 지원", "BOKJIRO_NATIONAL", null, null, false
        );
        List<String> keywords = List.of("자동차", "차량구입", "차량구매", "구입비", "교통");
        when(repository.findLifecycleCandidates(keywords, "서울특별시", "강남구"))
                .thenReturn(List.of(vehicle));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.VEHICLE_PURCHASE,
                "서울특별시",
                "강남구"
        );

        assertEquals(1, result.size());
        assertEquals("친환경 자동차 구입비 지원", result.get(0).getSupportName());
    }

    private List<String> childbirthKeywords() {
        return List.of("출산", "출생", "산모", "임산부", "신생아", "육아", "양육", "난임");
    }

    private WelfareSupportProduct product(
            Long id,
            String name,
            String sourceType,
            String sido,
            String sigungu,
            boolean complete
    ) {
        return WelfareSupportProduct.builder()
                .welfareSupportProductId(id)
                .productName(name)
                .sourceType(sourceType)
                .regionSido(sido)
                .regionSigungu(sigungu)
                .supportContent("출산 가정 지원")
                .applicationMethod(complete ? "온라인 신청" : null)
                .relatedUrl(complete ? "https://example.com/" + id : null)
                .build();
    }
}
