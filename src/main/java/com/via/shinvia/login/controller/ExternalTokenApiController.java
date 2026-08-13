package com.via.shinvia.login.controller;

import com.via.shinvia.login.security.CustomUserDetails;
import com.via.shinvia.mydata.client.MyDataAuthClient;
import com.via.shinvia.mydata.dto.TokenStatusResponse;
import com.via.shinvia.mydata.service.MyDataAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth/token")
@RequiredArgsConstructor
public class ExternalTokenApiController {

    private final MyDataAuthService tokenService;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final StringRedisTemplate stRedisTemplate;

    // 1. 모든 화면에서 호출할 토큰 상태 확인 API
    @GetMapping("/status")
    public ResponseEntity<TokenStatusResponse> getTokenStatus(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(new TokenStatusResponse(false, 0L));
        }

        Long userId = userDetails.getUserId();
        // Redis에서 남은 TTL(초) 조회
        Long remainingSeconds = tokenService.getAccessTokenTtl(userId);
        log.info("redisTokenTTl : " + remainingSeconds);

        boolean hasToken = remainingSeconds != null && remainingSeconds > 0;
        return ResponseEntity.ok(new TokenStatusResponse(hasToken, hasToken ? remainingSeconds : 0L));
    }

    // 2. [시간 연장 / 토큰 재발급] 버튼 클릭 시 호출할 API
    @PostMapping("/extend")
    public ResponseEntity<String> extendToken(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        Long userId = userDetails.getUserId();

        // 1) Redis의 외부 API Access Token 갱신 (Refresh Token 활용)
        String userKey = String.valueOf(userId);
        String refreshToken = stRedisTemplate.opsForValue().get("mydata:ci:rt:" + userKey);
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = stRedisTemplate.opsForValue().get(userKey);
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = "mock_rt_" + userKey + "_default";
        }

        try {
            tokenService.refreshToken(refreshToken);
        } catch (Exception e) {
            log.warn("[ExternalTokenApiController] 토큰 갱신 중 경고 발생 (userId: {}): {}", userId, e.getMessage());
        }

        // 2) 내 서비스의 JSESSIONID 세션 만료 시간도 같이 연장
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setMaxInactiveInterval(3600);
        }

        return ResponseEntity.ok("토큰 및 세션 시간이 성공적으로 연장되었습니다.");
    }
}