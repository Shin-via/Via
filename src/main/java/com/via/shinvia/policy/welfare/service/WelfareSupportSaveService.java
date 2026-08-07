package com.via.shinvia.policy.welfare.service;

import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.policy.welfare.repository.WelfareSupportProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
// 복합지원상품 일괄 저장 기능
public class WelfareSupportSaveService {
    private final WelfareSupportProductRepository repository;

    @Transactional
    public int saveAll(List<WelfareSupportProduct> products) {
        List<WelfareSupportProduct> validProducts = products.stream()
                .filter(product -> product.getExternalId() != null && product.getProductName() != null)
                .toList();
        if (validProducts.isEmpty()) {
            throw new IllegalStateException("복합지원 API 결과가 비어 있어 기존 데이터를 유지합니다.");
        }
        repository.deactivateAll();
        validProducts.forEach(repository::upsert);
        return validProducts.size();
    }
}
