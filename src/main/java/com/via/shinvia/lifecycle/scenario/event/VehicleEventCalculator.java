package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LoanRepaymentCalculator loanRepaymentCalculator;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.VEHICLE_PURCHASE;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        BigDecimal beforeCash = nvl(beforeState.getCashAsset());
        BigDecimal totalCost = nvl(input.getEstimatedCost());
        BigDecimal requiredAmount = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : totalCost;

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 1. 자기자금(선납금/취등록세) 현금 차감
        if (beforeCash.compareTo(requiredAmount) >= 0) {
            afterCash = beforeCash.subtract(requiredAmount);
            summary = String.format("차량 구매 자기자금으로 약 %s원이 지출되었습니다.", formatMoney(requiredAmount));
        } else {
            fundingShortage = requiredAmount.subtract(beforeCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("차량 구매 자기자금 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 2. 월 생활비 증가 (유지비 또는 카드 무이자할부금 등)
        BigDecimal additionalExpense = nvl(input.getAdditionalMonthlyExpense());
        BigDecimal newLivingExpense = nvl(beforeState.getMonthlyLivingExpense()).add(additionalExpense);

        // 3. 정식 금융 대출(오토론 등) 발생 시 부채 및 DSR 반영
        BigDecimal newLoanAmount = nvl(input.getNewLoanAmount());
        BigDecimal newTotalDebt = nvl(beforeState.getTotalDebt());
        BigDecimal newDebtPayment = nvl(beforeState.getMonthlyDebtPayment());

        if (newLoanAmount.compareTo(BigDecimal.ZERO) > 0) {
            newTotalDebt = newTotalDebt.add(newLoanAmount);
            
            // 5년(60개월) 연 5.0% 원리금균등 분할상환 계산
            try {
                var calcResult = loanRepaymentCalculator.calculate(
                        newLoanAmount,
                        new BigDecimal("5.0"),
                        60,
                        "원리금균등상환"
                );
                if (calcResult != null && calcResult.monthlyPayment() != null) {
                    newDebtPayment = newDebtPayment.add(calcResult.monthlyPayment());
                }
            } catch (Exception e) {
                BigDecimal monthlyPrincipal = newLoanAmount.divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
                newDebtPayment = newDebtPayment.add(monthlyPrincipal);
            }
        }

        // 4. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .totalDebt(newTotalDebt)
                .monthlyLivingExpense(newLivingExpense)
                .monthlyDebtPayment(newDebtPayment)
                .build();

        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = newDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[VehicleEventCalculator] Event calculated. Required: {}, NewLoan: {}, AdditionalExpense: {}, NewDSR: {}%",
                requiredAmount, newLoanAmount, additionalExpense, afterState.getDsr());

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.VEHICLE_PURCHASE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalCost)
                .supportBenefit(BigDecimal.ZERO)
                .fundingShortage(fundingShortage)
                .summary(summary)
                .build();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0만";
        BigDecimal manWon = amount.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(manWon) + "만";
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}

