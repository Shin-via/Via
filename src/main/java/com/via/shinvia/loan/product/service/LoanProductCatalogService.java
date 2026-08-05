package com.via.shinvia.loan.product.service;

import com.via.shinvia.loan.product.client.MockLoanProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;


// mock으로 부터 대출 상품들 가져오는 서비스단
// 대출 추천 서비스 로직이랑 겹치지 않게 CatalogService로 설정
@Service
@RequiredArgsConstructor
public class LoanProductCatalogService {

    private final MockLoanProductClient mockLoanProductClient;

    public Map<String, Object> getLoanProducts(
            String loanType,
            Boolean active
    ) {
        return mockLoanProductClient.getLoanProducts(
                loanType,
                active
        );
    }

    public Map<String, Object> getLoanProduct(
            Long loanProductId
    ) {
        return mockLoanProductClient.getLoanProduct(
                loanProductId
        );
    }
}