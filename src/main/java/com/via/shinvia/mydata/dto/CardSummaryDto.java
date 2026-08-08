package com.via.shinvia.mydata.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSummaryDto {
    private String cardName;
    private String maskedCardNumber;
}
