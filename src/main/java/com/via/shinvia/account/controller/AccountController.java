package com.via.shinvia.account.controller;

import com.via.shinvia.account.client.MockAccountClient;
import com.via.shinvia.account.dto.mock.MockAccountDtos.AccountListResponse;
import com.via.shinvia.account.dto.request.AccountSyncRequest;
import com.via.shinvia.account.dto.response.AccountSyncResult;
import com.via.shinvia.account.service.AccountSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountSyncService accountSyncService;
    private final MockAccountClient accountClient;

    public AccountController(
            AccountSyncService accountSyncService,
            MockAccountClient accountClient
    ) {
        this.accountSyncService = accountSyncService;
        this.accountClient = accountClient;
    }

    @Operation(summary = "계좌 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<AccountListResponse> list(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "next_page", required = false) String nextPage,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        AccountListResponse response = accountClient.getAccounts(authorization,nextPage, limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "계좌 동기화")
    @PostMapping("/sync")
    public ResponseEntity<AccountSyncResult> sync(
            @RequestBody AccountSyncRequest request
    ) {
        AccountSyncResult result = accountSyncService.sync(request);
        return ResponseEntity.ok(result);
    }
}