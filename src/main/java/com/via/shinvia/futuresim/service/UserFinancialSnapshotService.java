package com.via.shinvia.futuresim.service;

import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import com.via.shinvia.stresstest.service.AnnualIncomeProvider;
import com.via.shinvia.stresstest.service.LiquidAssetAggregator;
import com.via.shinvia.stresstest.service.LoanBurdenAggregator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// "지금 내 상태" 비교 패널에 쓸 사용자 실제 소득/자산/부채/순자산 스냅샷.
// stresstest/의 기존 서비스·매퍼를 그대로 재사용하고(수정 없음), stresstest 파일이 아닌
// 이 파일에서만 조합 로직을 둔다.
// - 소득: AnnualIncomeProvider (user_financial_profile.annual_income)
// - 자산: LiquidAssetAggregator (user_financial_profile.liquid_asset_amount, 유동자산 기준)
// - 부채(총 잔액): LoanBurdenAggregator는 월상환액만 주기 때문에(순자산 계산엔 원금 잔액이 필요),
//   StressTestLoanMapper.findNormalLoansByUserId()로 직접 조회해 current_balance를 합산한다.
// - monthlyLoanPayment: LoanBurdenAggregator(rateDeltaPercent=0)의 현재 월상환액 — 보조 참고값
// - annualDebtRepayment: monthlyLoanPayment * 12 — KOSIS avg/median_debt_repayment가 "전년도(연간)"
//   기준이라, 월상환액을 그대로 비교하면 단위가 안 맞아서 12를 곱해 연간으로 환산한 값
@Service
public class UserFinancialSnapshotService {

    private final AnnualIncomeProvider annualIncomeProvider;
    private final LiquidAssetAggregator liquidAssetAggregator;
    private final LoanBurdenAggregator loanBurdenAggregator;
    private final StressTestLoanMapper loanMapper;

    public UserFinancialSnapshotService(
            AnnualIncomeProvider annualIncomeProvider,
            LiquidAssetAggregator liquidAssetAggregator,
            LoanBurdenAggregator loanBurdenAggregator,
            StressTestLoanMapper loanMapper
    ) {
        this.annualIncomeProvider = annualIncomeProvider;
        this.liquidAssetAggregator = liquidAssetAggregator;
        this.loanBurdenAggregator = loanBurdenAggregator;
        this.loanMapper = loanMapper;
    }

    public Snapshot getSnapshot(Long userId) {
        AnnualIncomeProvider.Result income = annualIncomeProvider.findAnnualIncome(userId);
        LiquidAssetAggregator.Result asset = liquidAssetAggregator.aggregate(userId);
        BigDecimal totalDebt = sumLoanBalances(userId);
        LoanBurdenAggregator.Result loanBurden = loanBurdenAggregator.aggregate(userId, BigDecimal.ZERO);

        BigDecimal netWorth = asset.available()
                ? asset.totalLiquidAssets().subtract(totalDebt)
                : null;

        BigDecimal monthlyLoanPayment = loanBurden.totalCurrentLoanPayment();

        return new Snapshot(
                income.available() ? income.annualIncome() : null,
                asset.available() ? asset.totalLiquidAssets() : null,
                totalDebt,
                netWorth,
                monthlyLoanPayment,
                monthlyLoanPayment.multiply(BigDecimal.valueOf(12))
        );
    }

    private BigDecimal sumLoanBalances(Long userId) {
        List<StressTestLoanRow> loans = loanMapper.findNormalLoansByUserId(userId);

        BigDecimal total = BigDecimal.ZERO;
        for (StressTestLoanRow loan : loans) {
            total = total.add(loan.getCurrentBalance());
        }
        return total;
    }

    public record Snapshot(
            BigDecimal annualIncome,
            BigDecimal liquidAsset,
            BigDecimal totalDebt,
            BigDecimal netWorth,
            BigDecimal monthlyLoanPayment,
            BigDecimal annualDebtRepayment
    ) {
    }
}
