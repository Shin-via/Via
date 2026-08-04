package com.via.shinvia.mydata.service;

import com.via.shinvia.mydata.client.MyDataAuthClient;
import com.via.shinvia.mydata.dto.MyDataAuthTokenResponseDto;
import com.via.shinvia.mydata.dto.MyDataCommonResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 마이데이터 OAuth 연동 비즈니스 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyDataAuthService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(365);

    private final MyDataAuthClient myDataAuthClient;
    private final StringRedisTemplate redisTemplate;

    /**
     * 1. 인가코드 발급 URL 요청
     */
    public String getAuthorizeUrl(String userCi, String state) {
        return myDataAuthClient.requestAuthorize(userCi, state);
    }

    /**
     * 2. 인가코드로 Access Token 및 Refresh Token 발급 및 Redis 저장
     */
    public MyDataAuthTokenResponseDto issueTokens(String userCi, String code) {
        MyDataAuthTokenResponseDto response = myDataAuthClient.requestAccessToken(code);

        if (response != null && StringUtils.hasText(userCi)) {
            saveTokensToRedis(userCi, response.getAccessToken(), response.getRefreshToken());
        }

        return response;
    }

    /**
     * 3. Refresh Token으로 Access Token 갱신 및 Redis 저장
     */
    public MyDataAuthTokenResponseDto refreshToken(String userCi, String refreshToken) {
        MyDataAuthTokenResponseDto response = myDataAuthClient.refreshAccessToken(refreshToken);

        if (response != null && StringUtils.hasText(userCi)) {
            saveTokensToRedis(userCi, response.getAccessToken(), response.getRefreshToken());
        }

        return response;
    }

    /**
     * 4. 토큰 폐기
     */
    public MyDataCommonResponseDto revokeToken(String token, String revokeType) {
        return myDataAuthClient.revokeToken(token, "0");
    }

    /**
     * Redis 양방향 키-값 매핑 저장 (TTL: AccessToken=1시간, RefreshToken=1년)
     * 1) ci : accesstoken (mydata:ci:at:{ci} -> accessToken)
     * 2) ci : refreshtoken (mydata:ci:rt:{ci} -> refreshToken)
     * 3) accesstoken : ci (mydata:at:ci:{accessToken} -> ci)
     * 4) refreshtoken : ci (mydata:rt:ci:{refreshToken} -> ci)
     */
    public void saveTokensToRedis(String userCi, String accessToken, String refreshToken) {
        if (!StringUtils.hasText(userCi)) {
            log.warn("[Redis Token Storage] userCi가 없어 Redis에 저장하지 않습니다.");
            return;
        }

        if (StringUtils.hasText(accessToken)) {
            redisTemplate.opsForValue().set("mydata:ci:at:" + userCi, accessToken, ACCESS_TOKEN_TTL);
            redisTemplate.opsForValue().set("mydata:at:ci:" + accessToken, userCi, ACCESS_TOKEN_TTL);
        }

        if (StringUtils.hasText(refreshToken)) {
            redisTemplate.opsForValue().set("mydata:ci:rt:" + userCi, refreshToken, REFRESH_TOKEN_TTL);
            redisTemplate.opsForValue().set("mydata:rt:ci:" + refreshToken, userCi, REFRESH_TOKEN_TTL);
        }

        log.info("[Redis Token Storage] CI[{}] 토큰 4개 Key-Value 저장 완료 (Access TTL: 1시간, Refresh TTL: 365일)", userCi);
    }
}

