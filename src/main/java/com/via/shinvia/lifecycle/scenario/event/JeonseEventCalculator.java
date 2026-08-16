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
 * [전세 이벤트 계산기]
 * 1. 기존 주거 보증금(월세/전세) 회수 -> 현금 환원
 * 2. 신규 전세보증금 중 자기자금 차감 (부족 시 fundingShortage)
 * 3. 신규 전세보증금 총액을 housingAsset(보증금 자산)에 등록
 * 4. 전세대출 발생 시 만기일시상환(이자만 납부) 월 상환액 및 DSR 반영
 * 5. 월 주거비는 순수 관리비(additionalMonthlyExpense)로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JeonseEventCalculator implements LifecycleEventCalculator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private final LoanRepaymentCalculator loanRepaymentCalculator;

    @Override
    public LifecycleEventType getEventType() {
        return LifecycleEventType.JEONSE;
    }

    @Override
    public LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input) {
        if (beforeState == null || input == null) {
            return null;
        }

        // 1. 기존 주거 보증금 회수 (현금으로 전환)
        BigDecimal previousDeposit = nvl(beforeState.getHousingAsset());
        BigDecimal currentCash = nvl(beforeState.getCashAsset()).add(previousDeposit);

        // 2. 신규 전세보증금 및 자기자금/대출금 파악
        BigDecimal totalJeonseDeposit = nvl(input.getEstimatedCost());
        BigDecimal requiredCash = input.getUserRequiredAmount() != null 
                ? input.getUserRequiredAmount() 
                : totalJeonseDeposit;
        BigDecimal newLoanAmount = nvl(input.getNewLoanAmount());
        BigDecimal monthlyMaintenanceFee = nvl(input.getAdditionalMonthlyExpense()); // 전세 관리비

        BigDecimal afterCash;
        BigDecimal fundingShortage = BigDecimal.ZERO;
        String summary;

        // 3. 자기자금 지출 처리
        if (currentCash.compareTo(requiredCash) >= 0) {
            afterCash = currentCash.subtract(requiredCash);
            summary = String.format("전세보증금 자기자금 %s원 투입 및 입주가 완료되었습니다.", formatMoney(requiredCash));
        } else {
            fundingShortage = requiredCash.subtract(currentCash);
            afterCash = BigDecimal.ZERO;
            summary = String.format("전세 자기자금 중 약 %s원이 부족합니다.", formatMoney(fundingShortage));
        }

        // 4. 전세자금대출 발생 처리 (만기일시상환: 매월 이자만 납부, 기본 연 3.8% 가정)
        BigDecimal newTotalDebt = nvl(beforeState.getTotalDebt());
        BigDecimal newDebtPayment = nvl(beforeState.getMonthlyDebtPayment());

        if (newLoanAmount.compareTo(BigDecimal.ZERO) > 0) {
            newTotalDebt = newTotalDebt.add(newLoanAmount);
            try {
                var calcResult = loanRepaymentCalculator.calculate(
                        newLoanAmount,
                        new BigDecimal("3.8"), // 전세대출 평균 금리 3.8%
                        24,                    // 기본 2년(24개월) 만기
                        "만기일시상환"
                );
                if (calcResult != null && calcResult.monthlyPayment() != null) {
                    newDebtPayment = newDebtPayment.add(calcResult.monthlyPayment());
                }
            } catch (Exception e) {
                // 실패 시 간이 이자 계산 (대출금 * 3.8% / 12)
                BigDecimal monthlyInterest = newLoanAmount.multiply(new BigDecimal("0.038"))
                        .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
                newDebtPayment = newDebtPayment.add(monthlyInterest);
            }
        }

        // 5. 이벤트 직후 재정 상태(afterState) 생성
        LifecycleFinancialStateDto afterState = beforeState.toBuilder()
                .stateDate(input.getTargetDate() != null ? input.getTargetDate() : beforeState.getStateDate())
                .cashAsset(afterCash)
                .housingAsset(totalJeonseDeposit)              // 전세보증금 전액을 자산으로 등록
                .totalDebt(newTotalDebt)                       // 전세대출 부채 등록
                .monthlyHousingExpense(monthlyMaintenanceFee)  // 월세는 0원 되고 순수 관리비만 발생
                .monthlyDebtPayment(newDebtPayment)            // 전세대출 이자 상환액 반영
                .build();

        // 6. 월 저축여력 및 DSR 재계산
        afterState.recalculateMonthlySavingCapacity();
        if (afterState.getAnnualIncome() != null && afterState.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualPayment = newDebtPayment.multiply(BigDecimal.valueOf(12));
            afterState.setDsr(annualPayment.multiply(BigDecimal.valueOf(100)).divide(afterState.getAnnualIncome(), 2, RoundingMode.HALF_UP));
        }

        log.info("[JeonseEventCalculator] Event calculated. Deposit: {}, RequiredCash: {}, Loan: {}, NewDebtPayment: {}, Shortage: {}",
                totalJeonseDeposit, requiredCash, newLoanAmount, newDebtPayment, fundingShortage);

        return LifecycleEventResult.builder()
                .lifecycleEventId(input.getLifecycleEventId())
                .eventType(LifecycleEventType.JEONSE)
                .eventDate(input.getTargetDate())
                .beforeState(beforeState)
                .afterState(afterState)
                .eventCost(totalJeonseDeposit)
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

