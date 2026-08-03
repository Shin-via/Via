package com.via.shinvia.policy.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinancialProductDTO {

    private String id;
    private String searchText;
    private String title;
    private String badge;
    private String firstLabel;
    private String firstValue;
    private String secondLabel;
    private String secondValue;
    private String institution;
}
