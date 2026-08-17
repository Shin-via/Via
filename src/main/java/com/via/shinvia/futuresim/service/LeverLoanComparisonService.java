package com.via.shinvia.futuresim.service;

import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** 4단계 표 전용 대출 부담 비교. 상환 산식은 LoanRepaymentCalculator만 사용한다. */
@Service
public class LeverLoanComparisonService {
    private final StressTestLoanMapper loanMapper;
    private final LoanRepaymentCalculator calculator;
    private final FutureSimulationEngine simulationEngine;

    public LeverLoanComparisonService(StressTestLoanMapper loanMapper, LoanRepaymentCalculator calculator, FutureSimulationEngine simulationEngine) {
        this.loanMapper = loanMapper;
        this.calculator = calculator;
        this.simulationEngine = simulationEngine;
    }

    public record Summary(BigDecimal monthlyBurden, BigDecimal totalInterest, int repaymentPeriodMonths) {}

    public Summary baseline(Long userId) { return summarize(loanMapper.findNormalLoansByUserId(userId)); }

    private Summary summarize(List<StressTestLoanRow> loans) {
        BigDecimal monthly = BigDecimal.ZERO, interest = BigDecimal.ZERO; int longest = 0;
        for (StressTestLoanRow loan : loans) {
            int months = Math.max(1, calculator.calculateRemainingMonths(loan.getMaturityAt()));
            var result = calculator.calculate(loan.getCurrentBalance(), loan.getInterestRate(), months, loan.getRepaymentType());
            monthly = monthly.add(result.monthlyPayment()); interest = interest.add(result.totalInterest()); longest = Math.max(longest, months);
        }
        return new Summary(monthly, interest, longest);
    }

    public Summary forLever(Long userId, LeverIntensityCalculator.LeverType type, BigDecimal intensity) {
        List<StressTestLoanRow> loans = loanMapper.findNormalLoansByUserId(userId);
        if (type == LeverIntensityCalculator.LeverType.INCOME_CHANGE) {
            Summary base = summarize(loans);
            BigDecimal extraMonthlyIncome = simulationEngine.calculateSavingsCapacity(userId).monthlyIncome()
                    .multiply(intensity).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            // 계약상 대출 상환액은 같지만, 늘어난 소득을 상환 재원으로 본 '체감 월 부담'은 그만큼 줄어든다.
            return new Summary(base.monthlyBurden().subtract(extraMonthlyIncome), base.totalInterest(), base.repaymentPeriodMonths());
        }
        StressTestLoanRow target = loans.stream().max(Comparator.comparing(StressTestLoanRow::getCurrentBalance)).orElse(null);
        if (type == LeverIntensityCalculator.LeverType.NEW_LOAN) {
            Summary base = summarize(loans);
            var add = calculator.calculate(intensity, LeverIntensityCalculator.NEW_LOAN_ASSUMED_RATE_PERCENT,
                    LeverIntensityCalculator.NEW_LOAN_ASSUMED_TERM_MONTHS, LeverIntensityCalculator.NEW_LOAN_ASSUMED_REPAYMENT_TYPE);
            return new Summary(base.monthlyBurden().add(add.monthlyPayment()), base.totalInterest().add(add.totalInterest()),
                    Math.max(base.repaymentPeriodMonths(), LeverIntensityCalculator.NEW_LOAN_ASSUMED_TERM_MONTHS));
        }
        if (target == null) return summarize(loans);
        int months = Math.max(1, calculator.calculateRemainingMonths(target.getMaturityAt()));
        BigDecimal principal = type == LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT
                ? target.getCurrentBalance().subtract(intensity.min(target.getCurrentBalance())) : target.getCurrentBalance();
        int adjustedMonths = type == LeverIntensityCalculator.LeverType.LOAN_TERM_EXTENSION ? months + intensity.intValue() : months;
        BigDecimal monthly = BigDecimal.ZERO, interest = BigDecimal.ZERO; int longest = 0;
        for (StressTestLoanRow loan : loans) {
            int m = loan == target ? adjustedMonths : Math.max(1, calculator.calculateRemainingMonths(loan.getMaturityAt()));
            BigDecimal p = loan == target ? principal : loan.getCurrentBalance();
            var r = p.signum() == 0 ? null : calculator.calculate(p, loan.getInterestRate(), m, loan.getRepaymentType());
            monthly = monthly.add(r == null ? BigDecimal.ZERO : r.monthlyPayment()); interest = interest.add(r == null ? BigDecimal.ZERO : r.totalInterest()); longest = Math.max(longest, m);
        }
        return new Summary(monthly, interest, longest);
    }
}
