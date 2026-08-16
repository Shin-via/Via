package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * [월세 이벤트 계산기]
 * 1. 기존에 묶여있던 주거 보증금(전세/월세)이 있다면 현금으로 회수
 * 2. 신규 월세 보증금 차감 (부족 시 부족자금 산출) 및 housingAsset(보증금) 등록
 * 3. 매월 나가는 월 주거비(monthlyHousingExpense = 월세 + 관리비) 갱신
 */
@Slf4j
@Component
public class MonthlyRentEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.MONTHLY_RENT;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        // 1. 기존 주거 보증금 회수 (기존 전세/월세에서 살고 있었다면 보증금을 현금으로 환원)
        BigDecimal previousDeposit = nvl(beforeState.getHousingAsset());
        BigDecimal currentCash = nvl(beforeState.getCashAsset()).add(previousDeposit);

        // 2. 신규 월세 보증금 및 월세 비용 파악
        // estimatedCost: 신규 월세 보증금 (예: 1,000만 원)
        // additionalMonthlyExpense: 매월 나갈 월세 + 관리비 (예: 월 70만 원)
        BigDecimal newDeposit = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : nvl(input.getEstimatedCost());
        BigDecimal monthlyRentAndFee = nvl(input.getAdditionalMonthlyExpense());

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 3. 신규 보증금 지출 처리
        if (currentCash.compareTo(newDeposit) >= 0) {
            afterCash = currentCash.subtract(newDeposit);
            summary = String.format("월세 보증금 %s원 지출 및 월 주거비 %s원이 설정되었습니다.", 
                    formatMoney(newDeposit), formatMoney(monthlyRentAndFee));
        } else {
            fundingShortage = newDeposit.subtract(currentCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("월세 보증금 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 4. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .housingAsset(newDeposit)                       // 보증금 자산 등록
                .monthlyHousingExpense(monthlyRentAndFee)       // 매월 나갈 월 주거비(월세+관리비) 갱신
                .build();

        // 5. 월 저축여력 재계산 (월세가 늘어났으므로 저축여력 감소)
        afterState.recalculateMonthlySavingCapacity();

        log.info("[MonthlyRentEventCalculator] Event calculated. Deposit: {}, MonthlyRent: {}, AfterCash: {}, Shortage: {}",
                newDeposit, monthlyRentAndFee, afterCash, fundingShortage);

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.MONTHLY_RENT)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(newDeposit)
                .supportBenefit(BigDecimal.ZERO)
                .fundingShortage(fundingShortage)
                .summary(summary)
                .build();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0만";
        BigDecimal manWon = amount.divide(BigDecimal.valueOf(10000), 0, java.math.RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(manWon) + "만";
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}

