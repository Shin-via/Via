package com.via.shinvia.mydata.service;

import com.via.shinvia.mydata.client.MyDataCardClient;
import com.via.shinvia.mydata.client.dto.response.CardListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyDataCardService {

    private final MyDataConnectionService myDataConnectionService;
    private final MyDataAuthService myDataAuthService;
    private final MyDataCardClient myDataCardClient;

    public CardListResponseDto getCards(Long userId) {
        Long connectionId =
                myDataConnectionService.getConnectedConnectionId(userId);

        String accessToken =
                myDataAuthService.getAccessToken(connectionId);

        CardListResponseDto response =
                myDataCardClient.getCards(accessToken);

        System.out.println("cardCnt = " + response.getCardCnt());
        System.out.println("cardList = " + response.getCardList());

        return myDataCardClient.getCards(accessToken);
    }
}