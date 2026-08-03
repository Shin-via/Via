package com.via.shinvia.policy.asset.service;

import com.via.shinvia.policy.asset.client.AssetProductClient;
import com.via.shinvia.policy.asset.dto.AssetProductSearchDTO;
import com.via.shinvia.policy.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetProductService {
    private final AssetProductClient client;
    public FinancialProductPageDTO findAll(AssetProductSearchDTO search) { return client.findAll(search); }
    public FinancialProductDetailDTO findById(String id) { return client.findById(id); }
}
