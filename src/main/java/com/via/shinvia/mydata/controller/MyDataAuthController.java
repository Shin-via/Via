package com.via.shinvia.mydata.controller;

import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.mydata.dto.MyDataAuthTokenResponseDto;
import com.via.shinvia.mydata.dto.MyDataCommonResponseDto;
import com.via.shinvia.mydata.service.MyDataAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 마이데이터 OAuth 2.0 연동 및 토큰 수신 처리 컨트롤러
 * (OAuth 2.0 표준 규격: 1. 인가코드 요청(302 Redirect) -> 2. 콜백 수신 및 토큰 교환)
 */
@Slf4j
@RestController
@RequestMapping("/api/mydata/oauth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MyDataAuthController {

    private final MyDataAuthService myDataAuthService;
    private final MyDataProperties myDataProperties;

    /**
     * 1. 마이데이터 인가코드 허용 요청 (HTTP 302 Redirect)
     * 유저의 브라우저를 신한 목 서버의 인가 페이지로 리다이렉트시킵니다.
     * 예: GET http://localhost:8080/api/mydata/oauth/authorize?userCi=1
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
           @RequestParam(required = false, defaultValue = "1") String userCi)
            {
        log.info("[MyData Controller] 인가코드 요청 시작 - userCi: {}", userCi);

        // 신한 목 서버 302 Location URL 획득
        String mockAuthorizeUrl = myDataAuthService.getAuthorizeUrl(userCi);

        log.info("[MyData Controller] 목 서버 인가 URL로 리다이렉트(302): {}", mockAuthorizeUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(mockAuthorizeUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 2. 마이데이터 인가코드 콜백 수신 및 토큰 발급
     * 신한 목 서버가 인가코드(code)와 함께 리다이렉트해오는 콜백 엔드포인트
     */
    @GetMapping("/callback")
    public ResponseEntity<MyDataAuthTokenResponseDto> callback(
            @RequestHeader(value = "api_tran_id", required = false) String apiTranId,
            @RequestParam(value = "org_code", required = false) String orgCode,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam("code") String code) {
        String effectiveUserCi = (state != null && !state.isBlank()) ? state : "1";

        log.info("[MyData Controller] 인가코드 콜백 수신 - effectiveUserCi: {}, code: {}, state: {}, tranId: {}", effectiveUserCi, code, state, apiTranId);

        // 수신받은 인가코드(code)로 Access Token 및 Refresh Token 발급 요청 및 Redis 저장
        MyDataAuthTokenResponseDto tokenResponse = myDataAuthService.issueTokens(effectiveUserCi, code);

        log.info("[MyData Controller] 토큰 발급 성공 - AccessToken: {}, RefreshToken: {}",
                tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());

        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 3. Access Token 갱신 요청
     */
    @PostMapping("/refresh")
    public ResponseEntity<MyDataAuthTokenResponseDto> refresh(
            @RequestParam("refreshToken") String refreshToken) {
        log.info("[MyData Controller] Access Token 갱신 요청 - refreshToken: {}", refreshToken);

        MyDataAuthTokenResponseDto tokenResponse = myDataAuthService.refreshToken(refreshToken);

        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 4. 토큰 폐기 요청
     */
    @PostMapping("/revoke")
    public ResponseEntity<MyDataCommonResponseDto> revoke(
            @RequestParam("token") String token,
        @RequestParam("revoke_type") String revokeType)
    {
        log.info("[MyData Controller] 토큰 폐기 요청 - token: {}", token);
        MyDataCommonResponseDto response = myDataAuthService.revokeToken(token,revokeType);

        return ResponseEntity.ok(response);
    }
}
