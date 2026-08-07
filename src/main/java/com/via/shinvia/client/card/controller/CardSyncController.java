package com.via.shinvia.client.card.controller;

import com.via.shinvia.client.card.billdetail.MydataCardBillDetailClient;
import com.via.shinvia.client.card.billdetail.request.CardBillDetailRequest;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
import com.via.shinvia.client.card.list.MydataCardListClient;
import com.via.shinvia.client.card.list.request.CardListRequest;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.client.card.controller.dto.CardListSyncResponse;
import com.via.shinvia.client.card.controller.dto.CardTransactionSyncResponse;
import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.service.mydata.CardSyncService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 목서버 카드 API를 호출해 via_sys에 저장하는 과정을 Postman 등으로 수동 실행/검증하기 위한 컨트롤러.
 * TODO(회원인증/OAuth 동의 미구현): appUserId/mydataConnectionId를 요청 파라미터로 직접 받는다.
 * 로그인 세션·MyData 동의 플로우가 생기면 그쪽에서 값을 가져오도록 바꿔야 한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/cards/sync")
@RequiredArgsConstructor
public class CardSyncController {

    private final MydataCardListClient cardListClient;
    private final MydataCardBillDetailClient cardBillDetailClient;
    private final CardSyncService cardSyncService;
    private final StringRedisTemplate redisTemplate;
    private final MyDataProperties myDataProperties;
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String strTime = now.format(formatter);

    @Operation(summary = "카드 목록 동기화", security = @SecurityRequirement(name = "bearerAuth"))
    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<CardListSyncResponse> syncCardList(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization") String authorization,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(value = "x-api-type", defaultValue = "search") String type ){

        String token = extractAccessToken(authorization);
        log.info(" 수신된 Authorization 헤더: {}, 최종 token:{}", authorization, token);

        CardListResponse response = cardListClient.getCards(extractAccessToken(authorization), type, CardListRequest.builder()
                .searchTimestamp(strTime)
                .limit(limit)
                .build());

            String appUserId = redisTemplate.opsForValue().get(token);
        log.info("결과 : " +token + " "+ redisTemplate.opsForValue().get(token)+ " " + appUserId);
        var saved = cardSyncService.saveCards(response,appUserId);
        return ResponseEntity.ok(new CardListSyncResponse(response.getCardList(), saved));
    }

    @RequestMapping(value = "/transactions", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<CardTransactionSyncResponse> syncCardTransactions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String orgCode,
            @RequestParam String chargeMonth,
            @RequestParam(required = false) String seqno,
            @RequestParam(defaultValue = "20") Integer limit) {

        String token = (authorization != null && !authorization.isBlank()) ? authorization : "Bearer mock_access_token";

        CardBillDetailResponse response = cardBillDetailClient.getCardBillDetails(extractAccessToken(token), CardBillDetailRequest.builder()
                .orgCode(orgCode)
                .chargeMonth(chargeMonth)
                .seqno(seqno)
                .limit(limit)
                .build());

        var saved = cardSyncService.saveCardTransactions(response);
        return ResponseEntity.ok(new CardTransactionSyncResponse(response.getBillDetailList(), saved));
    }

    private String extractAccessToken(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return authorizationHeader;
    }
}
