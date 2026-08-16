package com.via.shinvia.surplusfund.calculation.service;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.client.card.mapper.CardMapper;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurplusFundService {
    private final MyDataConnectionService myDataConnectionService;
    private final AccountQueryService accountQueryService;
    private final CardMapper cardMapper;

    public BigDecimal calculateTotalCurrentBalance(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        List<Account> accounts = accountQueryService.getAccountsByConnectionId(connectionId);

        return accounts.stream()
                .map(Account::getCurrentBalance)
                .filter(balance -> balance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateScheduledCardAmount(Long userId) {

        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        BigDecimal amount = cardMapper.sumChargeAmountByUserAndMonth(userId, currentMonth);

        return amount != null ? amount : BigDecimal.ZERO;
    }

    public BigDecimal calculateAvailableSurplusAmount(Long userId) {

        BigDecimal totalCurrentBalance = calculateTotalCurrentBalance(userId);

        BigDecimal scheduledCardAmount = calculateScheduledCardAmount(userId);

        return totalCurrentBalance.subtract(scheduledCardAmount).max(BigDecimal.ZERO);
    }
}
