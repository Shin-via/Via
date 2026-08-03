package com.via.shinvia.policy.asset.client;

import com.via.shinvia.policy.asset.dto.AssetProductSearchDTO;
import com.via.shinvia.policy.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetProductClient {
    private final KinfaFinancialProductClient client;

    public FinancialProductPageDTO findAll(AssetProductSearchDTO search) {
        return client.findProducts(
                KinfaFinancialProductClient.ProductType.ASSET,
                search.getKeyword(), search.getPage(), search.getSize(), search.filters());
    }

    public FinancialProductDetailDTO findById(String id) {
        return client.findDetail(KinfaFinancialProductClient.ProductType.ASSET, id);
    }
}
