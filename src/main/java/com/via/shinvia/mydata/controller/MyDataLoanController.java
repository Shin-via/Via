package com.via.shinvia.mydata.controller;

import com.via.shinvia.loan.account.entity.LoanAccount;
import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.service.mydata.LoanAccountSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mydata/loans")
@RequiredArgsConstructor
public class MyDataLoanController {

    private final LoanAccountSyncService loanAccountSyncService;
    private final CurrentUser currentUser;

    @PostMapping("/sync")
    public ResponseEntity<List<LoanAccount>> syncLoans(
            Authentication authentication
    ) {

        /*
         * 로그인 사용자
         */
        Long userId =
                currentUser.getUserId(authentication);

        log.info(
                "[Loan Sync Controller] 보유대출 동기화 요청 - userId={}",
                userId
        );

        List<LoanAccount> loans =
                loanAccountSyncService
                        .syncLoans(userId);

        return ResponseEntity.ok(loans);
    }
}