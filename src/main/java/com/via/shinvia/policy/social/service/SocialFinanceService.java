package com.via.shinvia.policy.social.service;

import com.via.shinvia.policy.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.social.client.SocialFinanceClient;
import com.via.shinvia.policy.social.dto.SocialFinanceSearchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialFinanceService {
    private final SocialFinanceClient client;
    public FinancialProductPageDTO findAll(SocialFinanceSearchDTO search) { return client.findAll(search); }
    public FinancialProductDetailDTO findById(String id) { return client.findById(id); }
}
