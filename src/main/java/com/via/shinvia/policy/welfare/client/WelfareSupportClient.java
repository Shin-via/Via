package com.via.shinvia.policy.welfare.client;

import com.via.shinvia.policy.client.KinfaFinancialProductClient;
import com.via.shinvia.policy.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.welfare.dto.WelfareSupportSearchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 복합지원 외부 API 호출 기능
public class WelfareSupportClient {
    private final KinfaFinancialProductClient client;
    public FinancialProductPageDTO findAll(WelfareSupportSearchDTO search) {
        return client.findProducts(KinfaFinancialProductClient.ProductType.WELFARE,
                search.getKeyword(), search.getPage(), search.getSize(), search.filters());
    }
    public FinancialProductDetailDTO findById(String id) {
        return client.findDetail(KinfaFinancialProductClient.ProductType.WELFARE, id);
    }
}
