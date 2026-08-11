package com.via.shinvia.account.service;

import com.via.shinvia.account.client.MockAccountClient;
import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountItem;
import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountListResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicItem;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositBasicResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailItem;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositDetailResponse;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionItem;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionRequest;
import com.via.shinvia.account.dto.mock.MockAccountDtos.DepositTransactionResponse;
import com.via.shinvia.account.dto.request.AccountSyncRequest;
import com.via.shinvia.account.dto.response.AccountSyncResult;
import com.via.shinvia.account.model.Account;
import com.via.shinvia.account.model.AccountTransaction;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


// 전체적인 동기화 순서를 제어하는 서비스
@Service
public class AccountSyncService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MockAccountClient mockAccountClient;
    private final AccountDataConverter converter;
    private final AccountPersistenceService persistenceService;

    public AccountSyncService(
            MockAccountClient mockAccountClient,
            AccountDataConverter converter,
            AccountPersistenceService persistenceService
    ) {
        this.mockAccountClient = mockAccountClient;
        this.converter = converter;
        this.persistenceService = persistenceService;
    }

    public AccountSyncResult sync(
            AccountSyncRequest request
    ) {
        validateRequest(request);
        int limit = request.resolvedLimit();
        List<AccountItem> accountItems =
                fetchAllAccounts(
                        request.orgCode(),
                        limit
                );
        int syncedAccounts = 0;
        int skippedAccounts = 0;
        int insertedTransactions = 0;
        for (AccountItem accountItem : accountItems) {
            // isConset() 값이 true 인 경우에만 처리하는 로직
            if (!Boolean.TRUE.equals(
                    accountItem.isConsent()
            )) {
                skippedAccounts++;
                continue;
            }
            DepositBasicResponse basicResponse =
                    mockAccountClient.getDepositBasic(
                            new DepositBasicRequest(
                                    request.orgCode(),
                                    accountItem.accountNum(),
                                    accountItem.seqno(),
                                    "0"
                            )
                    );

            DepositDetailResponse detailResponse =
                    mockAccountClient.getDepositDetail(
                            new DepositDetailRequest(
                                    request.orgCode(),
                                    accountItem.accountNum(),
                                    accountItem.seqno(),
                                    "0"
                            )
                    );

            DepositBasicItem basicItem =
                    firstOrNull(
                            basicResponse.basicList()
                    );

            DepositDetailItem detailItem =
                    firstOrNull(
                            detailResponse.detailList()
                    );

            Account account = converter.toAccount(
                    request.orgCode(),
                    accountItem,
                    basicItem,
                    detailItem,
                    LocalDateTime.now()
            );

            List<DepositTransactionItem> mockTransactions =
                    fetchAllTransactions(
                            request,
                            accountItem,
                            limit
                    );

            List<AccountTransaction> transactions =
                    mockTransactions.stream()
                            .map(item ->
                                    converter.toTransaction(
                                            account.getExternalAccountKey(),
                                            item
                                    )
                            )
                            .toList();

            insertedTransactions +=
                    persistenceService.save(
                            account,
                            transactions
                    );

            syncedAccounts++;
        }

        return new AccountSyncResult(
                accountItems.size(),
                syncedAccounts,
                skippedAccounts,
                insertedTransactions
        );
    }

    private List<AccountItem> fetchAllAccounts(
            String orgCode,
            int limit
    ) {
        List<AccountItem> result =
                new ArrayList<>();

        String nextPage = null;

        do {
            AccountListResponse response =
                    mockAccountClient.getAccounts(
                            orgCode,
                            nextPage,
                            limit
                    );

            result.addAll(
                    safeList(response.accountList())
            );

            nextPage =
                    normalizeNextPage(
                            response.nextPage()
                    );

        } while (nextPage != null);

        return result;
    }

    private List<DepositTransactionItem>
    fetchAllTransactions(
            AccountSyncRequest request,
            AccountItem accountItem,
            int limit
    ) {
        List<DepositTransactionItem> result =
                new ArrayList<>();

        String nextPage = null;

        do {
            DepositTransactionResponse response =
                    mockAccountClient.getDepositTransactions(
                            new DepositTransactionRequest(
                                    request.orgCode(),
                                    accountItem.accountNum(),
                                    accountItem.seqno(),
                                    request.fromDate(),
                                    request.toDate(),
                                    nextPage,
                                    limit
                            )
                    );

            result.addAll(
                    safeList(response.transList())
            );

            nextPage =
                    normalizeNextPage(
                            response.nextPage()
                    );

        } while (nextPage != null);

        return result;
    }

    private void validateRequest(
            AccountSyncRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "요청값이 없습니다."
            );
        }

        if (request.orgCode() == null
                || request.orgCode().isBlank()) {
            throw new IllegalArgumentException(
                    "orgCode는 필수입니다."
            );
        }

        int limit = request.resolvedLimit();

        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException(
                    "limit은 1~500이어야 합니다."
            );
        }

        LocalDate fromDate = LocalDate.parse(
                request.fromDate(),
                DATE_FORMATTER
        );

        LocalDate toDate = LocalDate.parse(
                request.toDate(),
                DATE_FORMATTER
        );

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "fromDate는 toDate보다 늦을 수 없습니다."
            );
        }
    }

    private String normalizeNextPage(
            String nextPage
    ) {
        if (nextPage == null
                || nextPage.isBlank()) {
            return null;
        }

        return nextPage;
    }

    private <T> T firstOrNull(
            List<T> list
    ) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }

    private <T> List<T> safeList(
            List<T> list
    ) {
        return list == null
                ? Collections.emptyList()
                : list;
    }
}