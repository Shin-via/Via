package com.via.shinvia.policy.welfare.client;

import com.via.shinvia.policy.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.dto.FinancialProductDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.via.shinvia.policy.util.PolicyProductValues.*;

@Component
@RequiredArgsConstructor
// 복합지원상품 API 동기화 기능
public class WelfareSupportClient {
    private static final int FETCH_SIZE = 1000;
    private final KinfaFinancialProductClient client;

    public List<WelfareSupportProduct> fetchAll() {
        List<WelfareSupportProduct> result = new ArrayList<>();
        int pageNumber = 0;
        FinancialProductPageDTO page;

        do {
            page = client.findProducts(
                    KinfaFinancialProductClient.ProductType.WELFARE,
                    "", pageNumber, FETCH_SIZE);
            for (FinancialProductDTO product : page.getProducts()) {
                result.add(toEntity(product, client.findSourceDetail(
                        KinfaFinancialProductClient.ProductType.WELFARE, product.getId())));
            }
            pageNumber++;
        } while (!page.isLast());

        return result;
    }

    private WelfareSupportProduct toEntity(FinancialProductDTO item, Map<String, String> detail) {
        return WelfareSupportProduct.builder()
                .externalId(limit(item.getId(), 100))
                .productName(limit(join(first(detail, "spprtBizNm"), item.getTitle()), 200))
                .institutionName(limit(join(first(detail, "fndtnNm"), item.getInstitution()), 150))
                .supportTarget(join(first(detail, "trgtSttn"), first(detail, "detlSttn"),
                        item.getFirstValue()))
                .ageCondition(limit(join(first(detail, "age", "agegrp"), item.getSecondValue()), 200))
                .welfareType(limit(first(detail, "welfareType", "clsf"), 150))
                .supportContent(first(detail, "detlCtns"))
                .applicationMethod(join(first(detail, "aplyMthod"), first(detail, "rprsTelno")))
                .responsibleInstitution(limit(first(detail, "fndtnNm"), 200))
                .relatedUrl(limit(url(first(detail, "relatSite")), 500))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
