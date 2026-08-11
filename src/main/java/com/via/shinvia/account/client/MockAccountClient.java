package com.via.shinvia.account.client;

import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountListResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionResponse;
import com.via.shinvia.mydata.client.MyDataAuthClient;
import com.via.shinvia.mydata.config.MyDataProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MockAccountClient {
    private static final String SUCCESS_CODE = "00000";
    private final  MyDataAuthClient mydata;
    private final RestClient restClient;
    private final MyDataProperties myDataProperties;
    private final StringRedisTemplate redistemplate;

    public MockAccountClient(
            MyDataAuthClient mydata, RestClient.Builder restClientBuilder,
            @Value("${mock.account.base-url:http://localhost:9090}")
            String baseUrl, MyDataProperties myDataProperties, StringRedisTemplate redistemplate
    ) {
        this.mydata = mydata;
        this.myDataProperties = myDataProperties;
        this.redistemplate = redistemplate;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public AccountListResponse getAccounts(String authorization, String nextPage, int limit) {
        try {
            String token = (authorization != null && !authorization.isBlank()) ? authorization : "Bearer mock_access_token";
            String targetOrgCode = myDataProperties.getOrgCode();
            AccountListResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/v2/bank/accounts")
                                .queryParam("org_code", targetOrgCode)
                                .queryParam("search_timestamp","")
                                .queryParam("limit", limit);
                        if (hasText(nextPage)) {
                            builder.queryParam("next_page", nextPage);
                        }
                        return builder.build();
                    })
                    .header("authorization", token)
                    .header("x-api-tran-id", mydata.generateTranId())
                    .header("x-api-type"," ")
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

    /*public AccountListResponse getAccounts(String orgCode, String nextPage, int limit) {
        return getAccounts(null, nextPage, limit);
    }*/

    public DepositBasicResponse getDepositBasic(
            DepositBasicRequest request
    ) {
        return getDepositBasic(null, request);
    }

    public DepositBasicResponse getDepositBasic(
            String authorization,
            DepositBasicRequest request
    ) {
        try {
            String token = (authorization != null && !authorization.isBlank()) ? authorization : "Bearer mock_access_token";
            String effectiveOrgCode = (request != null && hasText(request.orgCode()))
                    ? request.orgCode()
                    : myDataProperties.getOrgCode();
            String effectiveSearchTimestamp = (request != null && hasText(request.searchTimestamp()))
                    ? request.searchTimestamp()
                    : "0";

            DepositBasicRequest finalRequest = new DepositBasicRequest(
                    effectiveOrgCode,
                    request != null ? request.accountNum() : null,
                    request != null ? request.seqno() : null,
                    effectiveSearchTimestamp
            );

            DepositBasicResponse response = restClient.post()
                    .uri("/v2/bank/accounts/deposit/basic")
                    .header("authorization", token)
                    .header("x-api-tran-id", mydata.generateTranId())
                    .header("x-api-type", "user")
                    .body(finalRequest)
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
        return getDepositDetail(null, request);
    }

    public DepositDetailResponse getDepositDetail(
            String authorization,
            DepositDetailRequest request
    ) {
        try {
            String token = (authorization != null && !authorization.isBlank()) ? authorization : "Bearer mock_access_token";
            String effectiveOrgCode = (request != null && hasText(request.orgCode()))
                    ? request.orgCode()
                    : myDataProperties.getOrgCode();
            String effectiveSearchTimestamp = (request != null && hasText(request.searchTimestamp()))
                    ? request.searchTimestamp()
                    : "0";

            DepositDetailRequest finalRequest = new DepositDetailRequest(
                    effectiveOrgCode,
                    request != null ? request.accountNum() : null,
                    request != null ? request.seqno() : null,
                    effectiveSearchTimestamp
            );

            DepositDetailResponse response = restClient.post()
                    .uri("/v2/bank/accounts/deposit/detail")
                    .header("authorization", token)
                    .header("x-api-tran-id", mydata.generateTranId())
                    .header("x-api-type", "user")
                    .body(finalRequest)
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
        return getDepositTransactions(null, request);
    }

    public DepositTransactionResponse getDepositTransactions(
            String authorization,
            DepositTransactionRequest request
    ) {
        try {
            String token = (authorization != null && !authorization.isBlank()) ? authorization : "Bearer mock_access_token";
            String effectiveOrgCode = (request != null && hasText(request.orgCode()))
                    ? request.orgCode()
                    : myDataProperties.getOrgCode();
            int limit = (request != null && request.limit() > 0) ? request.limit() : 20;

            DepositTransactionRequest finalRequest = new DepositTransactionRequest(
                    effectiveOrgCode,
                    request != null ? request.accountNum() : null,
                    request != null ? request.seqno() : null,
                    request != null ? request.fromDate() : null,
                    request != null ? request.toDate() : null,
                    request != null ? request.nextPage() : null,
                    limit
            );

            DepositTransactionResponse response = restClient.post()
                    .uri("/v2/bank/accounts/deposit/transactions")
                    .header("authorization", token)
                    .header("x-api-tran-id", mydata.generateTranId())
                    .header("x-api-type", "user")
                    .body(finalRequest)
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