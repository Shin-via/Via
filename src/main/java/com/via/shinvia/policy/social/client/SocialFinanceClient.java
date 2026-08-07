package com.via.shinvia.policy.social.client;

import com.via.shinvia.policy.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.dto.FinancialProductDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.social.entity.SocialFinanceProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.via.shinvia.policy.util.PolicyProductValues.*;

@Component
@RequiredArgsConstructor
// 사회연대금융상품 API 동기화 기능
public class SocialFinanceClient {
    private static final int FETCH_SIZE = 1000;
    private final KinfaFinancialProductClient client;

    public List<SocialFinanceProduct> fetchAll() {
        List<SocialFinanceProduct> result = new ArrayList<>();
        int pageNumber = 0;
        FinancialProductPageDTO page;

        do {
            page = client.findProducts(
                    KinfaFinancialProductClient.ProductType.SOCIAL,
                    "", pageNumber, FETCH_SIZE);
            for (FinancialProductDTO product : page.getProducts()) {
                result.add(toEntity(product, client.findSourceDetail(
                        KinfaFinancialProductClient.ProductType.SOCIAL, product.getId())));
            }
            pageNumber++;
        } while (!page.isLast());

        return result;
    }

    private SocialFinanceProduct toEntity(FinancialProductDTO item, Map<String, String> detail) {
        return SocialFinanceProduct.builder()
                .externalId(limit(item.getId(), 100))
                .productName(limit(join(first(detail, "spprtIsttNm"), item.getTitle()), 200))
                .institutionName(limit(join(first(detail, "ofrInsttNm"), item.getInstitution()), 150))
                .productCategory(limit(first(detail, "clsf"), 100))
                .supportTarget(join(first(detail, "spprtTrgt"), first(detail, "spprtTrgtDetlCnd")))
                .businessType(limit(join(first(detail, "projOverview"), first(detail, "rcritSchdl")), 200))
                .supportMethod(join(first(detail, "aplyMthod"), first(detail, "oprInstt")))
                .supportAmount(limit(first(detail, "spprtAmt", "lonNedAmt"), 200))
                .handlingInstitution(limit(join(first(detail, "mngeInstt"), first(detail, "oprInstt")), 200))
                .applicationMethod(join(first(detail, "aplyMthod"), first(detail, "inqy"),
                        first(detail, "etcNoitm")))
                .relatedUrl(limit(url(first(detail, "relatSite")), 500))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
