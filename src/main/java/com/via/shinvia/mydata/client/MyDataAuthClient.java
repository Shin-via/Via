package com.via.shinvia.mydata.client;

import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.mydata.dto.MyDataAuthTokenResponseDto;
import com.via.shinvia.mydata.dto.MyDataCommonResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Slf4j
@Component
public class MyDataAuthClient {

    private final RestClient restClient;
    private final MyDataProperties myDataProperties;

    public MyDataAuthClient(@Value("${shinvia-client.mock.url:http://localhost:9090}") String mockServerUrl,
                            MyDataProperties myDataProperties) {
        this.myDataProperties = myDataProperties;

        // 목 서버로 전송하는 HTTP 요청 및 응답 로깅 인터셉터
        ClientHttpRequestInterceptor loggingInterceptor = (request, body, execution) -> {
            log.info("\n======================= [HTTP OUTGOING REQUEST TO MOCK SERVER] =======================");
            log.info("Request URI    : {}", request.getURI());
            log.info("Request Method : {}", request.getMethod());
            log.info("Request Headers: {}", request.getHeaders());
            if (body != null && body.length > 0) {
                log.info("Request Body   : {}", new String(body, StandardCharsets.UTF_8));
            }
            log.info("=====================================================================================");

            ClientHttpResponse response = execution.execute(request, body);

            log.info("\n====================== [HTTP INCOMING RESPONSE FROM MOCK SERVER] =====================");
            log.info("Status Code    : {}", response.getStatusCode());
            log.info("Response Headers: {}", response.getHeaders());
            log.info("=====================================================================================\n");
            return response;
        };

        this.restClient = RestClient.builder()
                .baseUrl(mockServerUrl)
                .requestInterceptor(loggingInterceptor)
                .build();
    }


     // 1. 인가 코드 발급 요청 (GET /v2/oauth/2.0/authorize)
    public String requestAuthorize(String userCi ,String state) {
        String tranId = generateTranId();

        String effectiveClientId =  myDataProperties.getClientId();
        String effectiveRedirectUri = myDataProperties.getRedirectUri();
        String effectiveOrgCode = myDataProperties.getOrgCode();
        String effectiveState = (state != null && !state.isBlank()) ? state : "xyz123";

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/v2/oauth/2.0/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", effectiveClientId)
                .queryParam("redirect_uri", effectiveRedirectUri)
                .queryParam("org_code", effectiveOrgCode)
                .queryParam("state", effectiveState)
                .queryParam("app_scheme", myDataProperties.getAppScheme());

        log.info("[MyData Client] 인가코드 요청 - userCi: {}, orgCode: {}, tranId: {}", userCi, effectiveOrgCode, tranId);

        ResponseEntity<Void> response = restClient.get()
                .uri(uriBuilder.build().toUriString())
                .header("x-user-ci", userCi)
                .header("x-api-tran-id", tranId)
                .retrieve()
                .toBodilessEntity();

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        log.info("[MyData Client] 인가코드 발급 완료 - Location: {}", location);
        return location;
    }


     // 2. 접근 토큰 발급 요청 (POST /v2/oauth/2.0/token)
    public MyDataAuthTokenResponseDto requestAccessToken(String code) {
        String tranId = generateTranId();

        String effectiveClientId = myDataProperties.getClientId();
        String effectiveClientSecret =myDataProperties.getClientSecret();
        String effectiveOrgCode =  myDataProperties.getOrgCode();
        String effectiveRedirectUri =  myDataProperties.getRedirectUri();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        if (code != null) formData.add("code", code);
        formData.add("client_id", effectiveClientId);
        if (effectiveClientSecret != null) formData.add("client_secret", effectiveClientSecret);
        formData.add("org_code", effectiveOrgCode);
        formData.add("redirect_uri", effectiveRedirectUri);

        log.info("[MyData Client] Access Token 발급 요청 - code: {}, orgCode: {}, tranId: {}", code, effectiveOrgCode, tranId);

        MyDataAuthTokenResponseDto response = restClient.post()
                .uri("/v2/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("x-api-tran-id", tranId)
                .body(formData)
                .retrieve()
                .body(MyDataAuthTokenResponseDto.class);

        log.info("[MyData Client] Access Token 발급 완료 - accessToken: {}", response != null ? response.getAccessToken() : null);
        return response;
    }


     // 3. 접근 토큰 갱신 요청 (POST /v2/oauth/2.0/token - refresh_token)
    public MyDataAuthTokenResponseDto refreshAccessToken(String refreshToken) {
        String tranId = generateTranId();

        String effectiveClientId = myDataProperties.getClientId();
        String effectiveClientSecret =  myDataProperties.getClientSecret();
        String effectiveOrgCode = myDataProperties.getOrgCode();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        if (refreshToken != null) formData.add("refresh_token", refreshToken);
        formData.add("client_id", effectiveClientId);
        if (effectiveClientSecret != null) formData.add("client_secret", effectiveClientSecret);
        formData.add("org_code", effectiveOrgCode);
        formData.add("is_refreshed", "N");

        log.info("[MyData Client] Access Token 갱신 요청 - refreshToken: {}, orgCode: {}, tranId: {}", refreshToken, effectiveOrgCode, tranId);

        MyDataAuthTokenResponseDto response = restClient.post()
                .uri("/v2/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("x-api-tran-id", tranId)
                .body(formData)
                .retrieve()
                .body(MyDataAuthTokenResponseDto.class);

        log.info("[MyData Client] Access Token 갱신 완료 - newAccessToken: {}", response != null ? response.getAccessToken() : null);
        return response;
    }

    //4. 토큰 폐기 요청 (POST /v2/oauth/2.0/revoke)

    public MyDataCommonResponseDto revokeToken(String token, String revokeType) {
        String tranId = generateTranId();

        String effectiveClientId = myDataProperties.getClientId();
        String effectiveClientSecret = myDataProperties.getClientSecret();
        String effectiveOrgCode = myDataProperties.getOrgCode();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (token != null) formData.add("token", token);
        formData.add("client_id", effectiveClientId);
        if (effectiveClientSecret != null) formData.add("client_secret", effectiveClientSecret);
        formData.add("org_code", effectiveOrgCode);
        formData.add("revoke_type", revokeType != null ? revokeType : "0");

        log.info("[MyData Client] 토큰 폐기 요청 - token: {}, orgCode: {}, tranId: {}", token, effectiveOrgCode, tranId);

        MyDataCommonResponseDto response = restClient.post()
                .uri("/v2/oauth/2.0/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("x-api-tran-id", tranId)
                .body(formData)
                .retrieve()
                .body(MyDataCommonResponseDto.class);

        log.info("[MyData Client] 토큰 폐기 완료 - response: {}", response);
        return response;
    }

    private String generateTranId() {
        return "MOCK_TRAN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
