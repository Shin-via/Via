package com.via.shinvia.client.card.list.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardListRequest {

    private final String orgCode;
    private final String searchTimestamp;
    private final String nextPage;
    private final Integer limit;
}
