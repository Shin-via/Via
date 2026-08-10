package com.via.shinvia.policy.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRecommendationProfileDTO {

    private Long policyRecommendationProfileId;

    // 회원 식별자
    private Long userId;

    // =========================
    // 1. 거주 정보
    // =========================

    private String residenceSido;

    private String residenceSigungu;

    // 현재 지역 거주기간 (개월)
    private Integer residenceMonths;


    // =========================
    // 2. 근로 정보
    // =========================

    // 정규직 / 계약직 / 프리랜서 / 사업자 등
    private String employmentType;

    // 재직기간 (개월)
    private Integer employmentMonths;

    // 소득 존재 여부
    private Boolean hasIncome;

    // YES / NO / UNKNOWN
    private String incomeVerifiable;


    // =========================
    // 3. 가구 정보
    // =========================

    private Integer householdSize;

    // SINGLE / MARRIED / DIVORCED_ETC
    private String maritalStatus;

    private Boolean hasChildren;

    private Integer childrenCount;


    // =========================
    // 4. 복지 자격
    // =========================

    private Boolean basicLivelihoodRecipient;

    private Boolean nearPoverty;

    private Boolean singleParentHousehold;

    private Boolean disabled;

    private Boolean selfRelianceYouth;

    private Boolean multiculturalHousehold;


    // =========================
    // 5. 신용 / 금융
    // =========================

    // YES / NO / UNKNOWN
    private String debtDefaultStatus;

    // YES / NO / UNKNOWN
    private String overdueStatus;

    // NONE / CURRENT / PAST / UNKNOWN
    private String policyFinanceUsage;

    // YES / NO / UNKNOWN
    private String financialEducationStatus;


    // =========================
    // 6. 원하는 금융지원
    // =========================

    // LIVING / REFINANCE / HOUSING / BUSINESS / EDUCATION / ASSET / UNKNOWN
    private String desiredSupportPurpose;

    private BigDecimal desiredAmount;

    private BigDecimal monthlySavingCapacity;

    // RATE / LIMIT / PERIOD / BENEFIT / ELIGIBILITY
    private String priorityPreference;
}