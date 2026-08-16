package com.via.shinvia.mydata.controller;

import com.via.shinvia.client.card.list.MydataCardListClient;
import com.via.shinvia.client.card.list.request.CardListRequest;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.login.security.CustomUserDetails;
import com.via.shinvia.mydata.client.dto.response.CardListResponseDto;
import com.via.shinvia.mydata.service.MyDataAuthService;
import com.via.shinvia.mydata.service.MyDataCardService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.service.mydata.CardSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/mydata")
@RequiredArgsConstructor
public class MyDataViewController {

    private final MyDataCardService myDataCardService;
    private final CurrentUser currentUser;
    // 화면 표시(myDataCardService, 목서버 실시간 조회)와 별개로 card_account 저장까지 같이 해준다.
    // 기존엔 이 화면이 보여주기만 하고 저장을 안 해서, 연동 직후엔 DSR/스트레스테스트 등
    // card_account를 참조하는 다른 기능들이 데이터를 전혀 못 보는 문제가 있었다.
    private final MyDataConnectionService myDataConnectionService;
    private final MyDataAuthService myDataAuthService;
    private final MydataCardListClient mydataCardListClient;
    private final CardSyncService cardSyncService;

    @GetMapping("/result")
    public String resultPage(Authentication authentication, Model model) {
        Long userId=currentUser.getUserId(authentication);
        CardListResponseDto response = myDataCardService.getCards(userId);
        model.addAttribute("cards", response.getCardList());

        saveCardsQuietly(userId);

        return "mydata/result";
    }

    // 저장 실패가 연동 완료 화면 자체를 깨뜨리면 안 되므로 예외를 삼키고 로그만 남긴다.
    private void saveCardsQuietly(Long userId) {
        try {
            Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);
            String accessToken = myDataAuthService.getAccessToken(connectionId);

            CardListResponse syncResponse = mydataCardListClient.getCards(accessToken, CardListRequest.builder()
                    .searchTimestamp("0")
                    .limit(20)
                    .build());

            cardSyncService.saveCards(syncResponse, userId);
        } catch (Exception e) {
            log.warn("[MyData Result] 카드 저장 실패 (화면 표시엔 영향 없음) - userId={}, message={}", userId, e.getMessage());
        }
    }
}
