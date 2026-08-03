package com.via.shinvia.controller;

import com.via.shinvia.client.card.billdetail.MydataCardBillDetailClient;
import com.via.shinvia.client.card.billdetail.request.CardBillDetailRequest;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
import com.via.shinvia.client.card.list.MydataCardListClient;
import com.via.shinvia.client.card.list.request.CardListRequest;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.controller.dto.CardListSyncResponse;
import com.via.shinvia.controller.dto.CardTransactionSyncResponse;
import com.via.shinvia.service.mydata.CardSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 목서버 카드 API를 호출해 via_sys에 저장하는 과정을 Postman 등으로 수동 실행/검증하기 위한 컨트롤러.
 * TODO(회원인증/OAuth 동의 미구현): appUserId/mydataConnectionId를 요청 파라미터로 직접 받는다.
 * 로그인 세션·MyData 동의 플로우가 생기면 그쪽에서 값을 가져오도록 바꿔야 한다.
 */
@RestController
@RequestMapping("/api/cards/sync")
@RequiredArgsConstructor
public class CardSyncController {

    private final MydataCardListClient cardListClient;
    private final MydataCardBillDetailClient cardBillDetailClient;
    private final CardSyncService cardSyncService;

    @PostMapping("/list")
    public ResponseEntity<CardListSyncResponse> syncCardList(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Long appUserId,
            @RequestParam Long mydataConnectionId,
            @RequestParam String orgCode,
            @RequestParam(defaultValue = "0") String searchTimestamp,
            @RequestParam(defaultValue = "20") Integer limit) {

        CardListResponse response = cardListClient.getCards(extractAccessToken(authorization), CardListRequest.builder()
                .orgCode(orgCode)
                .searchTimestamp(searchTimestamp)
                .limit(limit)
                .build());

        var saved = cardSyncService.saveCards(appUserId, mydataConnectionId, orgCode, response);
        return ResponseEntity.ok(new CardListSyncResponse(response.getCardList(), saved));
    }

    @PostMapping("/transactions")
    public ResponseEntity<CardTransactionSyncResponse> syncCardTransactions(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String orgCode,
            @RequestParam String chargeMonth,
            @RequestParam(required = false) String seqno,
            @RequestParam(defaultValue = "20") Integer limit) {

        CardBillDetailResponse response = cardBillDetailClient.getCardBillDetails(extractAccessToken(authorization), CardBillDetailRequest.builder()
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
