package com.via.shinvia.policy.social.client;

import com.via.shinvia.policy.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.social.dto.SocialFinanceSearchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 사회연대금융 외부 API 호출 기능
public class SocialFinanceClient {
    private final KinfaFinancialProductClient client;
    public FinancialProductPageDTO findAll(SocialFinanceSearchDTO search) {
        return client.findProducts(KinfaFinancialProductClient.ProductType.SOCIAL,
                search.getKeyword(), search.getPage(), search.getSize(), search.filters());
    }
    public FinancialProductDetailDTO findById(String id) {
        return client.findDetail(KinfaFinancialProductClient.ProductType.SOCIAL, id);
    }
}
