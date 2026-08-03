package com.via.shinvia.policy.loan.service;

import com.via.shinvia.policy.loan.client.PolicySupportApiClient;
import com.via.shinvia.policy.loan.dto.api.LoanProductApiItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicySupportSyncService {

    private final PolicySupportApiClient apiClient;

    private final PolicySupportSaveService saveService;

    public int synchronize() {

        List<LoanProductApiItem> apiItems =
                apiClient.fetchAll();

        return saveService.saveAll(apiItems);
    }
}
