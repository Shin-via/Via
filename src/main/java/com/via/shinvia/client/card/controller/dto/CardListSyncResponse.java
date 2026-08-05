package com.via.shinvia.client.card.controller.dto;

import com.via.shinvia.client.card.list.response.CardInfoDto;
import com.via.shinvia.client.card.entity.CardAccount;

import java.util.List;

/**
 * 목서버에서 받은 원본 카드 목록(received)과 그중 card_account에 실제로 저장된 항목(saved)을 함께 보여준다.
 */
public record CardListSyncResponse(List<CardInfoDto> received, List<CardAccount> saved) {
}
