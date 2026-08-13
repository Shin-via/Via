package com.via.shinvia.lifecycle.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleProductDto {

    // 기존 금융상품 테이블의 식별자
    private Long productId;

    // 금융상품 유형
    // POLICY_LOAN, ASSET_FORMATION 등
    private String productType;

    // 상품명
    private String productName;

    // 취급 또는 운영기관
    private String institutionName;

    // 사용자 조건 기준 추천상태
    // ELIGIBLE, NEEDS_CONFIRMATION 등
    private String recommendationStatus;

    // 기존 추천엔진의 정렬용 추천점수
    private Integer recommendationScore;

    // 상품 금리 안내
    private String interestRate;

    // 대출 또는 지원 한도 안내
    private String loanLimit;

    // 대출 기간 안내
    private String loanPeriod;

    // 상환 방식 안내
    private String repaymentMethod;

    // 상품 상세페이지 또는 신청 URL
    private String relatedUrl;
}