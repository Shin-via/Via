package com.via.shinvia.loan.product.controller;

import com.via.shinvia.loan.product.service.LoanProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/loan-products")
@RequiredArgsConstructor
public class LoanProductCatalogController {

    private final LoanProductCatalogService service;

    /**
     * 서비스 서버를 통한 전체/유형별 대출상품 조회
     *
     * GET /api/loan-products
     * GET /api/loan-products?loan_type=CREDIT
     */
    @GetMapping
    public Map<String, Object> getLoanProducts(
            @RequestParam(
                    name = "loan_type",
                    required = false
            )
            String loanType,

            @RequestParam(
                    name = "active",
                    defaultValue = "true"
            )
            Boolean active
    ) {
        return service.getLoanProducts(
                loanType,
                active
        );
    }

    /**
     * 서비스 서버를 통한 대출상품 상세 조회
     *
     * GET /api/loan-products/{loanProductId}
     */
    @GetMapping("/{loanProductId}")
    public Map<String, Object> getLoanProduct(
            @PathVariable
            Long loanProductId
    ) {
        return service.getLoanProduct(
                loanProductId
        );
    }

}
