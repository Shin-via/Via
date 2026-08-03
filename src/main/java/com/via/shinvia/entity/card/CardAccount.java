package com.via.shinvia.entity.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardAccount {

    private Long cardAccountId;
    private Long appUserId; // TODO(회원인증 미구현): 현재는 CardSyncService 호출부가 파라미터로 채워줌
    private Long institutionId;
    private Long mydataConnectionId; // TODO(OAuth 동의 미구현): 현재는 CardSyncService 호출부가 파라미터로 채워줌
    private String externalCardKey;
    private String cardName;
    private String cardNumberMasked;
    private Integer paymentDay;
    private LocalDateTime updatedAt;
    private LocalDate issuedAt;
    private LocalDateTime dataAsOfAt;
}
