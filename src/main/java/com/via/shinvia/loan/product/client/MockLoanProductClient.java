package com.via.shinvia.loan.product.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class MockLoanProductClient {

    private final RestClient restClient;

    public MockLoanProductClient(
            @Value("${external.shinvia-mock.base-url}")
            String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Mock 서버의 대출상품 목록 조회
     */
    public Map<String, Object> getLoanProducts(
            String loanType,
            Boolean active
    ) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v2/loan-products");

                    if (loanType != null && !loanType.isBlank()) {
                        uriBuilder.queryParam(
                                "loan_type",
                                loanType.trim().toUpperCase()
                        );
                    }

                    if (active != null) {
                        uriBuilder.queryParam(
                                "active",
                                active
                        );
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                Map<String, Object>
                                >() {
                        }
                );
    }

    /**
     * Mock 서버의 대출상품 상세 조회
     */
    public Map<String, Object> getLoanProduct(
            Long loanProductId
    ) {
        return restClient.get()
                .uri(
                        "/v2/loan-products/{loanProductId}",
                        loanProductId
                )
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                Map<String, Object>
                                >() {
                        }
                );
    }
}