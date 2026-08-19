package com.via.shinvia.report.service.provider;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurplusFundCardProvider implements ReportCardDataProvider {

    private static final String CARD_KEY = "SURPLUS_FUND";

    @Override
    public String getCardKey() {
        return CARD_KEY;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public CardData getCardData(Long userId, Long refId) {
        return new CardData(CARD_KEY, "여유자금 운용", "준비중", "-", List.of(), "아직 연동되지 않은 카드예요.", null);
    }
}
