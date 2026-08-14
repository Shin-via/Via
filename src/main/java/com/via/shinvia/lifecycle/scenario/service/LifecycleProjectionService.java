package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleBaseStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleProjectionService {

    // 기본 물가상승률 (연 2.0%)
    private static final BigDecimal DEFAULT_INFLATION_RATE = new BigDecimal("0.02");

    /**
     * 1. 시뮬레이션 초기 금융상태(T0) 생성
     */
    public LifecycleFinancialStateDto createInitialState(LifecycleBaseStateDto baseState) {
        if (baseState == null) {
            return LifecycleFinancialStateDto.builder()
                    .stateDate(LocalDate.now())
                    .build();
        }

        // 대출 목록에서 총 부채 및 월 상환액 합산
        BigDecimal totalDebt = BigDecimal.ZERO;
        BigDecimal monthlyDebtPayment = BigDecimal.ZERO;

        List<LifecycleLoanDto> loans = baseState.getLoans();
        if (loans != null && !loans.isEmpty()) {
            for (LifecycleLoanDto loan : loans) {
                if (loan.getCurrentBalance() != null) {
                    totalDebt = totalDebt.add(loan.getCurrentBalance());
                }
                BigDecimal payment = calculateMonthlyPayment(loan);
                monthlyDebtPayment = monthlyDebtPayment.add(payment);
            }
        }

        // 초기 상태 객체 조립
        LifecycleFinancialStateDto initialState = LifecycleFinancialStateDto.builder()
                .stateDate(baseState.getBaseDate() != null ? baseState.getBaseDate() : LocalDate.now())
                .cashAsset(nvl(baseState.getLiquidAssetAmount()))
                .housingAsset(BigDecimal.ZERO)
                .totalDebt(totalDebt)
                .annualIncome(nvl(baseState.getAnnualIncome()))
                .monthlySupportIncome(BigDecimal.ZERO)
                .monthlyLivingExpense(nvl(baseState.getMonthlyLivingExpense()))
                .monthlyHousingExpense(nvl(baseState.getMonthlyHousingExpense()))
                .monthlyDebtPayment(monthlyDebtPayment)
                .build();

        // 초기 월 저축여력 계산
        initialState.recalculateMonthlySavingCapacity();

        log.info("[LifecycleProjectionService] Initial state created for userId: {}, cashAsset: {}, monthlySavingCapacity: {}",
                baseState.getUserId(), initialState.getCashAsset(), initialState.getMonthlySavingCapacity());

        return initialState;
    }

    /**
     * 2. 시간 전진 (T1 -> T2 시점으로 소득/물가 상승 및 매월 저축액 누적)
     *
     * @param currentState          현재 시점의 금융 상태
     * @param targetDate            목표 시점 (다음 이벤트 날짜)
     * @param annualSalaryGrowthRate 연간 급여 상승률 (예: 0.03 = 3%)
     * @param annualInflationRate   연간 물가 상승률 (예: 0.02 = 2%, null일 경우 기본 2%)
     * @return 목표 시점의 갱신된 금융 상태
     */
    public LifecycleFinancialStateDto project(
            LifecycleFinancialStateDto currentState,
            LocalDate targetDate,
            BigDecimal annualSalaryGrowthRate,
            BigDecimal annualInflationRate
    ) {
        if (currentState == null || targetDate == null) {
            return currentState;
        }

        LocalDate startDate = currentState.getStateDate();
        if (startDate == null || !targetDate.isAfter(startDate)) {
            // 과거 또는 동일 시점이면 날짜만 복제하여 반환
            return currentState.copy(targetDate);
        }

        // 경과 개월 수 계산
        Period period = Period.between(startDate, targetDate);
        int totalMonths = (int) java.time.temporal.ChronoUnit.MONTHS.between(startDate, targetDate);
        if (totalMonths <= 0) {
            return currentState.copy(targetDate);
        }

        BigDecimal years = BigDecimal.valueOf(totalMonths)
                .divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

        BigDecimal salaryRate = annualSalaryGrowthRate != null ? annualSalaryGrowthRate : BigDecimal.ZERO;
        BigDecimal inflationRate = annualInflationRate != null ? annualInflationRate : DEFAULT_INFLATION_RATE;

        // 1) 소득 상승 반영: Income * (1 + rate)^years (간이 선형 근사: Income * (1 + rate * years))
        BigDecimal incomeMultiplier = BigDecimal.ONE.add(salaryRate.multiply(years));
        BigDecimal projectedAnnualIncome = currentState.getAnnualIncome().multiply(incomeMultiplier)
                .setScale(0, RoundingMode.HALF_UP);

        // 2) 생활비 물가상승 반영: LivingExpense * (1 + inflation * years)
        BigDecimal expenseMultiplier = BigDecimal.ONE.add(inflationRate.multiply(years));
        BigDecimal projectedLivingExpense = currentState.getMonthlyLivingExpense().multiply(expenseMultiplier)
                .setScale(0, RoundingMode.HALF_UP);

        // 3) 경과 기간 동안의 월평균 저축 여력 계산하여 현금 자산에 누적
        // (초기 저축여력과 종료 시점 저축여력의 평균치로 기간 누적)
        BigDecimal startMonthlyIncome = currentState.getAnnualIncome().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal endMonthlyIncome = projectedAnnualIncome.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal startMonthlySaving = startMonthlyIncome
                .add(currentState.getMonthlySupportIncome())
                .subtract(currentState.getMonthlyLivingExpense())
                .subtract(currentState.getMonthlyHousingExpense())
                .subtract(currentState.getMonthlyDebtPayment());

        BigDecimal endMonthlySaving = endMonthlyIncome
                .add(currentState.getMonthlySupportIncome())
                .subtract(projectedLivingExpense)
                .subtract(currentState.getMonthlyHousingExpense())
                .subtract(currentState.getMonthlyDebtPayment());

        BigDecimal avgMonthlySaving = startMonthlySaving.add(endMonthlySaving)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        BigDecimal accumulatedSavings = avgMonthlySaving.multiply(BigDecimal.valueOf(totalMonths));

        // 저축액 누적 (단, 마이너스 저축인 경우 현금 감소)
        BigDecimal projectedCashAsset = currentState.getCashAsset().add(accumulatedSavings);

        // 새로운 상태 객체 생성
        LifecycleFinancialStateDto projectedState = currentState.toBuilder()
                .stateDate(targetDate)
                .cashAsset(projectedCashAsset)
                .annualIncome(projectedAnnualIncome)
                .monthlyLivingExpense(projectedLivingExpense)
                .build();

        // 최종 저축여력 재계산
        projectedState.recalculateMonthlySavingCapacity();

        log.info("[LifecycleProjectionService] Projected from {} to {} ({} months). Cash: {} -> {}, SavingCapacity: {}",
                startDate, targetDate, totalMonths, currentState.getCashAsset(), projectedCashAsset, projectedState.getMonthlySavingCapacity());

        return projectedState;
    }

    /**
     * 대출별 월 상환액 계산 (원리금균등, 원금균등, 만기일시)
     */
    public BigDecimal calculateMonthlyPayment(LifecycleLoanDto loan) {
        if (loan == null || loan.getCurrentBalance() == null || loan.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal balance = loan.getCurrentBalance();
        BigDecimal annualRate = loan.getInterestRate() != null ? loan.getInterestRate() : new BigDecimal("0.04"); // 기본 4%
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);

        String repaymentType = loan.getRepaymentType() != null ? loan.getRepaymentType().toUpperCase() : "MATURITY";

        // 만기일까지 남은 개월 수 (만기일 없으면 기본 60개월 가정)
        int remainingMonths = 60;
        if (loan.getMaturityAt() != null && loan.getMaturityAt().isAfter(LocalDate.now())) {
            remainingMonths = (int) java.time.temporal.ChronoUnit.MONTHS.between(LocalDate.now(), loan.getMaturityAt());
            if (remainingMonths <= 0) remainingMonths = 1;
        }

        if (repaymentType.contains("원리금") || repaymentType.contains("LEVEL") || repaymentType.contains("EQUAL_PRINCIPAL_AND_INTEREST")) {
            // 원리금균등: P * r * (1+r)^n / ((1+r)^n - 1)
            if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
                return balance.divide(BigDecimal.valueOf(remainingMonths), 0, RoundingMode.HALF_UP);
            }
            double r = monthlyRate.doubleValue();
            double n = remainingMonths;
            double factor = Math.pow(1 + r, n);
            double payment = balance.doubleValue() * (r * factor) / (factor - 1);
            return BigDecimal.valueOf(payment).setScale(0, RoundingMode.HALF_UP);

        } else if (repaymentType.contains("원금") || repaymentType.contains("EQUAL_PRINCIPAL")) {
            // 원금균등 (첫달 기준 근사치: P/n + P*r)
            BigDecimal principalPayment = balance.divide(BigDecimal.valueOf(remainingMonths), 0, RoundingMode.HALF_UP);
            BigDecimal interestPayment = balance.multiply(monthlyRate).setScale(0, RoundingMode.HALF_UP);
            return principalPayment.add(interestPayment);

        } else {
            // 만기일시 (이자만 납부: P * r)
            return balance.multiply(monthlyRate).setScale(0, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
