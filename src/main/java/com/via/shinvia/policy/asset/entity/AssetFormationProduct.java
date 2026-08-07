package com.via.shinvia.policy.asset.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
// 자산형성상품 DB 모델
public class AssetFormationProduct {
    private Long assetFormationProductId;
    private String externalId;
    private String productName;
    private String institutionName;
    private String subscriptionTarget;
    private String subscriptionPeriod;
    private String incomeCondition;
    private String ageCondition;
    private String supportRegion;
    private String savingMethod;
    private String governmentSupport;
    private String maturityBenefit;
    private String applicationMethod;
    private String relatedUrl;
    private Boolean active;
    private LocalDateTime sourceUpdatedAt;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
