package com.via.shinvia.client.card.list.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CardInfoDto {

    @JsonProperty("card_id")
    private String cardId;

    @JsonProperty("bank_code_std")
    private Long institution_id;

    @JsonProperty("card_number_masked")
    private String cardNum;

    @JsonProperty("is_consent")
    private Boolean isConsent;

    @JsonProperty("card_name")
    private String cardName;

    @JsonProperty("card_member")
    private String cardMember;

}
