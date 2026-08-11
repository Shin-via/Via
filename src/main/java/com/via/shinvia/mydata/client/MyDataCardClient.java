package com.via.shinvia.mydata.client;

import com.via.shinvia.mydata.client.dto.response.CardListResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MyDataCardClient {

    private final RestClient restClient;
    private final String orgCode;

    public MyDataCardClient(
            RestClient.Builder builder,
            @Value("${shinvia-client.mock.url}") String mockUrl,
            @Value("${shinvia-client.mydata.org-code}") String orgCode
    ) {
        this.restClient = builder.baseUrl(mockUrl).build();
        this.orgCode = orgCode;
    }

    public CardListResponseDto getCards(String accessToken) {
        return restClient.get().uri(uriBuilder -> uriBuilder.path("/v2/card/cards")
                                                                    .queryParam("org_code", orgCode)
                                                                    .queryParam("search_timestamp", "0")
                                                                    .queryParam("limit", 100)
                                                                    .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(CardListResponseDto.class);
    }
}