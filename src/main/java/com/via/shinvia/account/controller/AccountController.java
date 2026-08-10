package com.via.shinvia.account.controller;

import com.via.shinvia.account.dto.request.AccountSyncRequest;
import com.via.shinvia.account.dto.response.AccountListResult;
import com.via.shinvia.account.dto.response.AccountSyncResult;
import com.via.shinvia.account.service.AccountSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountSyncService accountSyncService;

    public AccountController(
            AccountSyncService accountSyncService
    ) {
        this.accountSyncService = accountSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<AccountSyncResult> sync(
            @RequestBody AccountSyncRequest request
    ) {
        AccountSyncResult result =
                accountSyncService.sync(request);

        return ResponseEntity.ok(result);
    }
}