package com.via.shinvia.surplusfund.calculation.service;

import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.service.AccountQueryService;
import com.via.shinvia.client.card.bill.MydataCardBillClient;
import com.via.shinvia.client.card.bill.request.CardBillRequest;
import com.via.shinvia.client.card.bill.response.CardBillDto;
import com.via.shinvia.client.card.bill.response.CardBillResponse;
import com.via.shinvia.mydata.config.MyDataProperties;
import com.via.shinvia.mydata.service.MyDataAuthService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SurplusFundService {
    private final MyDataConnectionService myDataConnectionService;
    private final AccountQueryService accountQueryService;
    private final MyDataAuthService myDataAuthService;
    private final MydataCardBillClient mydataCardBillClient;
    private final MyDataProperties myDataProperties;

    public BigDecimal calculateTotalCurrentBalance(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        List<Account> accounts = accountQueryService.getAccountsByConnectionId(connectionId);

        return accounts.stream()
                .map(Account::getCurrentBalance)
                .filter(balance -> balance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateScheduledCardAmount(Long userId) {

        Long connectionId = myDataConnectionService.getConnectedConnectionId(userId);

        String accessToken = myDataAuthService.getAccessToken(connectionId);

        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        CardBillRequest request = CardBillRequest.builder()
                .orgCode(myDataProperties.getOrgCode())
                .fromMonth(currentMonth)
                .toMonth(currentMonth)
                .limit(100)
                .build();

        CardBillResponse response = mydataCardBillClient.getCardBills(accessToken, request);

        if (response == null
                || response.getBillList() == null
                || response.getBillList().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return response.getBillList().stream()
                .map(CardBillDto::getChargeAmt)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateAvailableSurplusAmount(Long userId) {

        BigDecimal totalCurrentBalance = calculateTotalCurrentBalance(userId);

        BigDecimal scheduledCardAmount = calculateScheduledCardAmount(userId);

        return totalCurrentBalance
                .subtract(scheduledCardAmount)
                .max(BigDecimal.ZERO);
    }
}
