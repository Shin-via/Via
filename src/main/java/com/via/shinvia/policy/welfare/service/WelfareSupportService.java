package com.via.shinvia.policy.welfare.service;

import com.via.shinvia.policy.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import com.via.shinvia.policy.welfare.client.WelfareSupportClient;
import com.via.shinvia.policy.welfare.dto.WelfareSupportSearchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 복합지원 조회 처리 기능
public class WelfareSupportService {
    private final WelfareSupportClient client;
    public FinancialProductPageDTO findAll(WelfareSupportSearchDTO search) { return client.findAll(search); }
    public FinancialProductDetailDTO findById(String id) { return client.findById(id); }
}
