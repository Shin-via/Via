package com.via.shinvia.client.card.list;

import com.via.shinvia.client.card.list.request.CardListRequest;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.config.MockServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class MydataCardListClient {

    private final RestTemplate mydataRestTemplate;
    private final MockServerProperties mockServerProperties;

    public CardListResponse getCards(String accessToken, CardListRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(mockServerProperties.getBaseUrl())
                .path("/v2/card/cards")
                .queryParam("org_code", request.getOrgCode())
                .queryParam("limit", request.getLimit());

        if (StringUtils.hasText(request.getSearchTimestamp())) {
            builder.queryParam("search_timestamp", request.getSearchTimestamp());
        }
        if (StringUtils.hasText(request.getNextPage())) {
            builder.queryParam("next_page", request.getNextPage());
        }

        URI uri = builder.build().toUri();
        return mydataRestTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(authHeaders(accessToken)), CardListResponse.class)
                .getBody();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
