package com.via.shinvia.account.client;

import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountListResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MockAccountClient {

    private static final String SUCCESS_CODE = "00000";

    private final RestClient restClient;

    public MockAccountClient(
            RestClient.Builder restClientBuilder,
            @Value("${mock.account.base-url:http://localhost:9090}")
            String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public AccountListResponse getAccounts(
            String orgCode,
            String nextPage,
            int limit
    ) {
        try {
            AccountListResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/v2/bank/accounts")
                                .queryParam("org_code", orgCode)
                                .queryParam("search_timestamp", "0")
                                .queryParam("limit", limit);

                        if (hasText(nextPage)) {
                            builder.queryParam("next_page", nextPage);
                        }

                        return builder.build();
                    })
                    .retrieve()
                    .body(AccountListResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-001 계좌 목록 조회"
            );

            return response;

        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "은행-001 계좌 목록 조회 호출 실패",
                    exception
            );
        }
    }

    public DepositBasicResponse getDepositBasic(
            DepositBasicRequest request
    ) {
        try {
            DepositBasicResponse response = restClient.post()
                    .uri("/v2/bank/accounts/deposit/basic")
                    .body(request)
                    .retrieve()
                    .body(DepositBasicResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-002 수신계좌 기본정보 조회"
            );

            return response;

        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "은행-002 수신계좌 기본정보 조회 호출 실패",
                    exception
            );
        }
    }

    public DepositDetailResponse getDepositDetail(
            DepositDetailRequest request
    ) {
        try {
            DepositDetailResponse response = restClient.post()
                    .uri("/v2/bank/accounts/deposit/detail")
                    .body(request)
                    .retrieve()
                    .body(DepositDetailResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-003 수신계좌 추가정보 조회"
            );

            return response;

        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "은행-003 수신계좌 추가정보 조회 호출 실패",
                    exception
            );
        }
    }

    public DepositTransactionResponse getDepositTransactions(
            DepositTransactionRequest request
    ) {
        try {
            DepositTransactionResponse response = restClient.post()
                    .uri("/v2/bank/accounts/deposit/transactions")
                    .body(request)
                    .retrieve()
                    .body(DepositTransactionResponse.class);

            validate(
                    response == null ? null : response.rspCode(),
                    response == null ? null : response.rspMsg(),
                    "은행-004 수신계좌 거래내역 조회"
            );

            return response;

        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "은행-004 수신계좌 거래내역 조회 호출 실패",
                    exception
            );
        }
    }

    private void validate(
            String rspCode,
            String rspMsg,
            String apiName
    ) {
        if (rspCode == null) {
            throw new IllegalStateException(
                    apiName + " 응답이 비어 있습니다."
            );
        }

        if (!SUCCESS_CODE.equals(rspCode)) {
            throw new IllegalStateException(
                    apiName + " 실패: " + rspCode + " / " + rspMsg
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}