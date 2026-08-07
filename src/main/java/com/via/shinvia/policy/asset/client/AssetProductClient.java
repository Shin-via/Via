package com.via.shinvia.policy.asset.client;

import com.via.shinvia.policy.asset.entity.AssetFormationProduct;
import com.via.shinvia.policy.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.dto.FinancialProductDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.via.shinvia.policy.util.PolicyProductValues.*;

@Component
@RequiredArgsConstructor
// 자산형성상품 API 동기화 기능
public class AssetProductClient {
    private static final int FETCH_SIZE = 1000;
    private final KinfaFinancialProductClient client;

    public List<AssetFormationProduct> fetchAll() {
        List<AssetFormationProduct> result = new ArrayList<>();
        int pageNumber = 0;
        FinancialProductPageDTO page;

        do {
            page = client.findProducts(
                    KinfaFinancialProductClient.ProductType.ASSET,
                    "", pageNumber, FETCH_SIZE);
            for (FinancialProductDTO product : page.getProducts()) {
                result.add(toEntity(product, client.findSourceDetail(
                        KinfaFinancialProductClient.ProductType.ASSET, product.getId())));
            }
            pageNumber++;
        } while (!page.isLast());

        return result;
    }

    private AssetFormationProduct toEntity(FinancialProductDTO item, Map<String, String> detail) {
        return AssetFormationProduct.builder()
                .externalId(limit(item.getId(), 100))
                .productName(limit(join(first(detail, "fincPrdNm"), item.getTitle()), 200))
                .institutionName(limit(join(first(detail, "ofrInsttNm"), item.getInstitution()), 150))
                .subscriptionTarget(join(first(detail, "trgt"), first(detail, "spprtSpprtDetlCnd")))
                .subscriptionPeriod(limit(first(detail, "joinPrid"), 100))
                .incomeCondition(first(detail, "incmeRcgnzAmnt"))
                .ageCondition(limit(first(detail, "age"), 200))
                .supportRegion(limit(first(detail, "rsdnZone"), 200))
                .savingMethod(join(first(detail, "ipawy"), first(detail, "prdDs")))
                .governmentSupport(limit(join(first(detail, "svnAmt"),
                        first(detail, "highestInrtMxmMtcnAmt")), 200))
                .maturityBenefit(join(first(detail, "prdChrct"), first(detail, "etcNoitm")))
                .applicationMethod(join(first(detail, "etcMthod"),
                        first(detail, "trtmInsttVal"), first(detail, "inqy")))
                .relatedUrl(limit(url(first(detail, "relatSite")), 500))
                .active(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
