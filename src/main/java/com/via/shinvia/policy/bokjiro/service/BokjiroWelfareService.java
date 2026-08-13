package com.via.shinvia.policy.bokjiro.service;

import com.via.shinvia.policy.bokjiro.client.BokjiroApiClient;
import com.via.shinvia.policy.bokjiro.dto.BokjiroDetailResponseDTO;
import com.via.shinvia.policy.bokjiro.dto.BokjiroListRequestDTO;
import com.via.shinvia.policy.bokjiro.dto.BokjiroListResponseDTO;
import com.via.shinvia.policy.bokjiro.entity.BokjiroEntity;
import com.via.shinvia.policy.bokjiro.repository.BokjiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BokjiroWelfareService {

    private final BokjiroApiClient bokjiroApiClient;
    private final BokjiroRepository bokjiroRepository;

    public BokjiroListResponseDTO searchWelfareList(BokjiroListRequestDTO request) {
        log.info("복지로 복지서비스 목록 조회 서비스 실행: {}", request);
        return bokjiroApiClient.fetchWelfareList(request);
    }

    public BokjiroDetailResponseDTO getWelfareDetail(String servId) {
        log.info("복지로 복지서비스 상세 조회 서비스 실행: servId={}", servId);
        return bokjiroApiClient.fetchWelfareDetail(servId);
    }

    public long getSavedDbCount() {
        return bokjiroRepository.count();
    }

    public List<BokjiroEntity> getSavedDbList() {
        return bokjiroRepository.findAll();
    }
}
