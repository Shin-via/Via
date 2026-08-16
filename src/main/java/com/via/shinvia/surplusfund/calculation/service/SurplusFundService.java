package com.via.shinvia.surplusfund.calculation.service;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurplusFundService {
    private final MyDataConnectionService myDataConnectionService;
    private final AccountQueryService accountQueryService;

    public BigDecimal calculateTotalCurrentBalance(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        List<Account> accounts = accountQueryService.getAccountsByConnectionId(connectionId);

        return accounts.stream()
                .map(Account::getCurrentBalance)
                .filter(balance -> balance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
