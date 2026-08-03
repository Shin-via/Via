package com.via.shinvia.service.mydata;

import com.via.shinvia.client.card.billdetail.response.CardBillDetailDto;
import com.via.shinvia.client.card.billdetail.response.CardBillDetailResponse;
import com.via.shinvia.client.card.list.response.CardInfoDto;
import com.via.shinvia.client.card.list.response.CardListResponse;
import com.via.shinvia.entity.card.CardAccount;
import com.via.shinvia.entity.card.CardTransaction;
import com.via.shinvia.mapper.card.CardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * mock 서버 카드 목록/청구 상세 응답을 card_account/card_transaction 테이블에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class CardSyncService {

    private static final DateTimeFormatter PAID_DTIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CardMapper cardMapper;

    // TODO(회원인증/OAuth 동의 미구현): appUserId는 로그인 세션에서, mydataConnectionId는
    // MyData 동의(OAuth) 플로우에서 나와야 하는데 아직 둘 다 없어서 호출부가 직접 넘겨주는
    // 임시값이다. 인증/동의 플로우가 생기면 파라미터로 받는 대신 그 결과에서 조회하도록 바꿀 것.
    @Transactional
    public List<CardAccount> saveCards(Long appUserId, Long mydataConnectionId, String bankCodeStd, CardListResponse response) {
        Long institutionId = cardMapper.findInstitutionIdByOrgCode(bankCodeStd);
        if (institutionId == null) {
            throw new IllegalStateException("등록되지 않은 금융기관 코드입니다: " + bankCodeStd);
        }

        List<CardInfoDto> cardList = response.getCardList();
        if (cardList == null || cardList.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<CardAccount> saved = new ArrayList<>();
        for (CardInfoDto dto : cardList) {
            saved.add(upsertCard(appUserId, institutionId, mydataConnectionId, dto, now));
        }
        return saved;
    }

    private CardAccount upsertCard(Long appUserId, Long institutionId, Long mydataConnectionId, CardInfoDto dto, LocalDateTime now) {
        CardAccount existing = cardMapper.findByExternalCardKey(dto.getCardId());

        CardAccount cardAccount = CardAccount.builder()
                .cardAccountId(existing != null ? existing.getCardAccountId() : null)
                .appUserId(appUserId) // TODO(회원인증 미구현): 로그인 세션의 실제 사용자 ID로 교체
                .institutionId(institutionId)
                .mydataConnectionId(mydataConnectionId) // TODO(OAuth 동의 미구현): 실제 mydata_connection 조회 결과로 교체
                .externalCardKey(dto.getCardId())
                .cardName(dto.getCardName())
                .cardNumberMasked(dto.getCardNum())
                .updatedAt(now)
                .dataAsOfAt(now)
                .build();

        if (existing == null) {
            cardMapper.insertCardAccount(cardAccount);
        } else {
            cardMapper.updateCardAccount(cardAccount);
        }
        return cardAccount;
    }

    /**
     * 카드-005(GET /v2/card/bills/detail) 응답을 card_transaction에 upsert 한다.
     * card_id는 저장하지 않고 card_account.external_card_key 조회를 통해 card_account_id로 변환해서 저장하며,
     * total_install_cnt/cur_install_cnt/balance_amt는 card_transaction에 대응 컬럼이 없어 읽지 않는다.
     */
    @Transactional
    public List<CardTransaction> saveCardTransactions(CardBillDetailResponse response) {
        List<CardBillDetailDto> billDetailList = response.getBillDetailList();
        if (billDetailList == null || billDetailList.isEmpty()) {
            return List.of();
        }

        Map<String, Long> cardAccountIdByExternalCardKey = new HashMap<>();
        List<CardTransaction> transactions = billDetailList.stream()
                .map(dto -> toEntity(dto, cardAccountIdByExternalCardKey))
                .toList();

        cardMapper.upsertCardTransactions(transactions);
        return transactions;
    }

    private CardTransaction toEntity(CardBillDetailDto dto, Map<String, Long> cardAccountIdByExternalCardKey) {
        Long cardAccountId = cardAccountIdByExternalCardKey.computeIfAbsent(dto.getCardId(), this::findCardAccountId);

        return CardTransaction.builder()
                .cardAccountId(cardAccountId)
                .externalTransactionId(dto.getTransNo())
                .transactionAt(LocalDateTime.parse(dto.getPaidDtime(), PAID_DTIME_FORMATTER))
                .amount(dto.getPaidAmt())
                .merchantName(dto.getMerchantName())
                .build();
    }

    private Long findCardAccountId(String externalCardKey) {
        Long cardAccountId = cardMapper.findCardAccountIdByExternalCardKey(externalCardKey);
        if (cardAccountId == null) {
            throw new IllegalStateException("등록되지 않은 카드입니다: " + externalCardKey);
        }
        return cardAccountId;
    }
}
