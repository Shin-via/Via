package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * [대출 상환 이벤트 계산기]
 * 1. 통장 현금으로 기존 대출의 일부 또는 전액을 조기 상환
 * 2. 총 부채(totalDebt) 감소 처리
 * 3. 빚이 줄어든 비율에 비례하여 월 대출상환액(monthlyDebtPayment) 축소 (전액 상환 시 0원)
 * 4. DSR 대폭 감소 및 월 저축여력 상승 반영
 */
@Slf4j
@Component
public class RepaymentEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.REPAYMENT;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        BigDecimal beforeDebt = nvl(beforeState.getTotalDebt());
        BigDecimal beforePayment = nvl(beforeState.getMonthlyDebtPayment());

        // 사용자가 상환하고자 하는 희망 금액 (예: 2,000만 원)
        BigDecimal targetRepayAmount = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : nvl(input.getEstimatedCost());

        // 실제 빚보다 많이 갚으려 하면 빚 잔액만큼만 상환
        BigDecimal actualRepayTarget = targetRepayAmount.min(beforeDebt);

        BigDecimal afterCash;
        BigDecimal actualRepaidAmount;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 1. 현금 잔고 내에서 상환 처리
        if (beforeCash.compareTo(actualRepayTarget) >= 0) {
            afterCash = beforeCash.subtract(actualRepayTarget);
            actualRepaidAmount = actualRepayTarget;
            summary = String.format("기존 대출 중 약 %s원을 조기 상환하였습니다.", formatMoney(actualRepaidAmount));
        } else {
            // 현금이 부족한 경우 통장 잔고만큼만 최대한 상환
            actualRepaidAmount = beforeCash;
            fundingShortage = actualRepayTarget.subtract(beforeCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("가용 현금 %s원으로 대출을 일부 상환하였습니다. (희망액 대비 %s원 부족)", 
                    formatMoney(actualRepaidAmount), formatMoney(fundingShortage));
        }

        // 2. 부채 잔액 감소
        BigDecimal afterTotalDebt = beforeDebt.subtract(actualRepaidAmount).max(BigDecimal.ZERO);

        // 3. 월 대출 상환액 비율 축소 (전액 상환 시 0원)
        BigDecimal afterDebtPayment;
        if (afterTotalDebt.compareTo(BigDecimal.ZERO) == 0 || beforeDebt.compareTo(BigDecimal.ZERO) == 0) {
            afterDebtPayment = BigDecimal.ZERO;
        } else {
            // 남은 빚 비율만큼 월 상환액 축소: beforePayment * (afterTotalDebt / beforeDebt)
            BigDecimal remainingRatio = afterTotalDebt.divide(beforeDebt, 6, RoundingMode.HALF_UP);
            afterDebtPayment = beforePayment.multiply(remainingRatio).setScale(0, RoundingMode.HALF_UP);
        }

        // 4. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .totalDebt(afterTotalDebt)
                .monthlyDebtPayment(afterDebtPayment)
                .build();

        // 5. 월 저축여력 및 DSR 재계산 (상환액이 줄어 저축여력 상승 + DSR 대폭 개선)
        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = afterDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[RepaymentEventCalculator] Event calculated. Repaid: {}, RemainingDebt: {}, NewPayment: {}, NewDSR: {}%",
                actualRepaidAmount, afterTotalDebt, afterDebtPayment, afterState.getDsr());

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.REPAYMENT)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(actualRepaidAmount)
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

