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

/**
 * [주택구매 이벤트 계산기]
 * 1. 기존에 묶여있던 전세/월세 보증금을 전액 회수하여 주택 계약금/자기자금에 보탬
 * 2. 주택 매매 자기자금 + 세금/부대비용 차감 (부족 시 fundingShortage 산출)
 * 3. 매매 주택가격을 housingAsset(내 집 부동산 자산)으로 등록
 * 4. 주택담보대출(30년 원리금균등분할상환) 발생 시 월 상환액 및 DSR 반영
 * 5. 월 주거비는 아파트 관리비(additionalMonthlyExpense)로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HomePurchaseEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LoanRepaymentCalculator loanRepaymentCalculator;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.HOME_PURCHASE;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        // 1. 기존 주거 보증금 전액 회수 (현금으로 전환)
        BigDecimal previousDeposit = nvl(beforeState.getHousingAsset());
        BigDecimal currentCash = nvl(beforeState.getCashAsset()).add(previousDeposit);

        // 2. 주택 매매 총액, 자기자금, 주담대, 관리비 파악
        BigDecimal homePrice = nvl(input.getEstimatedCost()); // 주택 매매가 (예: 6억 원)
        BigDecimal requiredCash = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : homePrice; // 필요한 자기자금 (예: 2억 원)
        BigDecimal mortgageLoanAmount = nvl(input.getNewLoanAmount()); // 주담대 (예: 4억 원)
        BigDecimal monthlyMaintenanceFee = nvl(input.getAdditionalMonthlyExpense()); // 아파트 관리비

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 3. 자기자금 지출 처리 및 부족자금 계산
        if (currentCash.compareTo(requiredCash) >= 0) {
            afterCash = currentCash.subtract(requiredCash);
            summary = String.format("주택구매 자기자금 %s원 투입 및 주택 취득이 완료되었습니다.", formatMoney(requiredCash));
        } else {
            fundingShortage = requiredCash.subtract(currentCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("주택구매 시 자기자금이 약 %s원 부족합니다.", formatMoney(fundingShortage));
        }

        // 4. 주택담보대출 발생 처리 (30년/360개월, 연 4.2% 원리금균등분할상환 가정)
        BigDecimal newTotalDebt = nvl(beforeState.getTotalDebt());
        BigDecimal newDebtPayment = nvl(beforeState.getMonthlyDebtPayment());

        if (mortgageLoanAmount.compareTo(BigDecimal.ZERO) > 0) {
            newTotalDebt = newTotalDebt.add(mortgageLoanAmount);
            try {
                var calcResult = loanRepaymentCalculator.calculate(
                        mortgageLoanAmount,
                        new BigDecimal("4.2"), // 주담대 평균 금리 4.2%
                        360,                   // 30년 (360개월)
                        "원리금균등상환"
                );
                if (calcResult != null && calcResult.monthlyPayment() != null) {
                    newDebtPayment = newDebtPayment.add(calcResult.monthlyPayment());
                }
            } catch (Exception e) {
                // 폴백: 360개월 단순 분할 + 이자
                BigDecimal monthlyPrincipal = mortgageLoanAmount.divide(BigDecimal.valueOf(360), 0, RoundingMode.HALF_UP);
                BigDecimal monthlyInterest = mortgageLoanAmount.multiply(new BigDecimal("0.042"))
                        .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
                newDebtPayment = newDebtPayment.add(monthlyPrincipal).add(monthlyInterest);
            }
        }

        // 5. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .housingAsset(homePrice)                      // 매매 주택가격을 내 자산으로 등록
                .totalDebt(newTotalDebt)                      // 주담대 부채 등록
                .monthlyHousingExpense(monthlyMaintenanceFee) // 아파트 관리비로 갱신
                .monthlyDebtPayment(newDebtPayment)           // 30년 주담대 상환액 반영
                .build();

        // 6. 월 저축여력 및 DSR 재계산
        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = newDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[HomePurchaseEventCalculator] Event calculated. Price: {}, RequiredCash: {}, Mortgage: {}, NewDebtPayment: {}, Shortage: {}, DSR: {}%",
                homePrice, requiredCash, mortgageLoanAmount, newDebtPayment, fundingShortage, afterState.getDsr());

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.HOME_PURCHASE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(homePrice)
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

