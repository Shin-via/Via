package com.via.shinvia.policy.bokjiro.service;

import com.via.shinvia.policy.bokjiro.client.BokjiroApiClient;
import com.via.shinvia.policy.bokjiro.dto.BokjiroListResponseDTO;
import com.via.shinvia.policy.bokjiro.entity.BokjiroEntity;
import com.via.shinvia.policy.bokjiro.repository.BokjiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BokjiroSyncLoader implements ApplicationRunner {

    private final BokjiroApiClient bokjiroApiClient;
    private final BokjiroRepository bokjiroRepository;

    @Value("${finance.api.sync-on-startup:true}")
    private boolean syncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnStartup) {
            log.info("복지로 API 시작 시 자동 동기화 비활성화 상태");
            return;
        }

        try {
            int count = synchronizeBokjiroData();
            log.info("복지로 API 동기화 확인 완료: 총 {}건 DB(bokjiro 테이블) 유지/저장 상태", count);
        } catch (Exception e) {
            log.warn("복지로 API 동기화 중 안내 (기존 DB 데이터 유지): {}", e.getMessage());
        }
    }

    @Transactional
    public int synchronizeBokjiroData() {
        // DB 테이블이 없을 경우 자동으로 DDL 생성
        bokjiroRepository.createTableIfNotExists();

        long existingCount = bokjiroRepository.count();
        if (existingCount > 0) {
            log.info("DB(bokjiro)에 이미 {}건의 복지 정책 데이터가 정상 저장되어 있어 트래픽 절약을 위해 시작 시 동기화를 건너뜁니다.", existingCount);
            return (int) existingCount;
        }

        List<BokjiroListResponseDTO.WelfareItem> items;
        try {
            items = bokjiroApiClient.fetchAllWelfareList();
        } catch (Exception e) {
            log.warn("복지로 API 수신 중 제한 안내 (일일 트래픽 초과 또는 API 제한): {}", e.getMessage());
            return 0;
        }

        if (items == null || items.isEmpty()) {
            log.warn("복지로 API 응답 데이터가 비어 있어 동기화를 건너뜁니다.");
            return 0;
        }

        bokjiroRepository.deactivateAll();

        int count = 0;
        for (BokjiroListResponseDTO.WelfareItem item : items) {
            if (item.getServId() == null || item.getServNm() == null) {
                continue;
            }

            Integer inqNum = null;
            if (item.getInqNum() != null) {
                try {
                    inqNum = Integer.parseInt(item.getInqNum().trim());
                } catch (NumberFormatException ignored) {}
            }

            BokjiroEntity entity = BokjiroEntity.builder()
                    .servId(item.getServId())
                    .servNm(item.getServNm())
                    .jurMnofNm(item.getJurMnofNm())
                    .jurOrgNm(item.getJurOrgNm())
                    .inqNum(inqNum)
                    .servDgst(item.getServDgst())
                    .servDtlLink(item.getServDtlLink())
                    .svcfrstRegTs(item.getSvcfrstRegTs())
                    .lifeArray(item.getLifeArray())
                    .intrsThemaArray(item.getIntrsThemaArray())
                    .trgterIndvdlArray(item.getTrgterIndvdlArray())
                    .sprtCycNm(item.getSprtCycNm())
                    .srvPvsnNm(item.getSrvPvsnNm())
                    .rprsCtadr(item.getRprsCtadr())
                    .onapPsbltYn(item.getOnapPsbltYn())
                    .active(true)
                    .syncedAt(LocalDateTime.now())
                    .build();

            bokjiroRepository.upsert(entity);
            count++;
        }

        return count;
    }
}
