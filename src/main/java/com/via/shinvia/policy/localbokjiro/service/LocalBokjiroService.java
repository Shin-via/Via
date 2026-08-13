package com.via.shinvia.policy.localbokjiro.service;

import com.via.shinvia.policy.localbokjiro.client.LocalBokjiroApiClient;
import com.via.shinvia.policy.localbokjiro.dto.LocalBokjiroDetailResponseDTO;
import com.via.shinvia.policy.localbokjiro.dto.LocalBokjiroListRequestDTO;
import com.via.shinvia.policy.localbokjiro.dto.LocalBokjiroListResponseDTO;
import com.via.shinvia.policy.localbokjiro.entity.LocalBokjiroEntity;
import com.via.shinvia.policy.localbokjiro.repository.LocalBokjiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalBokjiroService {

    private final LocalBokjiroApiClient localBokjiroApiClient;
    private final LocalBokjiroRepository localBokjiroRepository;

    public LocalBokjiroListResponseDTO searchLocalWelfareList(LocalBokjiroListRequestDTO request) {
        log.info("localbokjiro 지자체 복지서비스 목록 조회 서비스 실행: {}", request);
        return localBokjiroApiClient.fetchLocalWelfareList(request);
    }

    public LocalBokjiroDetailResponseDTO getLocalWelfareDetail(String servId) {
        log.info("localbokjiro 지자체 복지서비스 상세 조회 서비스 실행: servId={}", servId);
        return localBokjiroApiClient.fetchLocalWelfareDetail(servId);
    }

    public long getSavedDbCount() {
        return localBokjiroRepository.count();
    }

    public List<LocalBokjiroEntity> getSavedDbList() {
        return localBokjiroRepository.findAll();
    }
}
