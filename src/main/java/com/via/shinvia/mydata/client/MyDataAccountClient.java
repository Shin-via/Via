package com.via.shinvia.mydata.client;

import com.via.shinvia.mydata.client.dto.response.BankAccountsResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MyDataAccountClient {
    private final RestClient restClient;

    public MyDataAccountClient(RestClient.Builder restClientBuilder, @Value("${mydata.mock-server.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public BankAccountsResponseDto getAccounts(String orgCode, Integer limit) {
        return restClient.get().uri(uriBuilder -> uriBuilder.path("/v2/bank/accounts").queryParam("org_code", orgCode)
                .queryParam("limit", limit).build()).retrieve().body(BankAccountsResponseDto.class);
    }
}
