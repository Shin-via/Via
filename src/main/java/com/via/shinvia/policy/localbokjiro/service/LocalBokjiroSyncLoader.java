package com.via.shinvia.policy.localbokjiro.service;

import com.via.shinvia.policy.localbokjiro.client.LocalBokjiroApiClient;
import com.via.shinvia.policy.localbokjiro.dto.LocalBokjiroListResponseDTO;
import com.via.shinvia.policy.localbokjiro.entity.LocalBokjiroEntity;
import com.via.shinvia.policy.localbokjiro.repository.LocalBokjiroRepository;
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
public class LocalBokjiroSyncLoader implements ApplicationRunner {

    private final LocalBokjiroApiClient localBokjiroApiClient;
    private final LocalBokjiroRepository localBokjiroRepository;

    @Value("${finance.api.sync-on-startup:true}")
    private boolean syncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnStartup) {
            log.info("localbokjiro 지자체 복지 API 시작 시 자동 동기화 비활성화 상태");
            return;
        }

        try {
            int count = synchronizeLocalBokjiroData();
            log.info("localbokjiro 지자체 복지 API 시작 시 자동 동기화 확인 완료: 총 {}건 DB(localbokjiro 테이블) 저장/유지 완료", count);
        } catch (Exception e) {
            log.warn("localbokjiro 지자체 복지 API 시작 시 자동 동기화 처리 안내 (공공데이터포털 활용신청/트래픽 한도 확인 필요): {}", e.getMessage());
        }
    }

    @Transactional
    public int synchronizeLocalBokjiroData() {
        // DB 테이블이 없을 경우 자동으로 DDL 생성
        localBokjiroRepository.createTableIfNotExists();

        long existingCount = localBokjiroRepository.count();
        if (existingCount > 0) {
            log.info("DB(localbokjiro)에 이미 {}건의 지자체 복지 데이터가 저장되어 있어 시작 시 자동 동기화를 건너뜁니다.", existingCount);
            return (int) existingCount;
        }

        List<LocalBokjiroListResponseDTO.LocalWelfareItem> items;
        try {
            items = localBokjiroApiClient.fetchAllLocalWelfareList();
        } catch (Exception e) {
            log.warn("localbokjiro API 수신 중 제한 안내 (일일 트래픽 초과 또는 API 제한): {}", e.getMessage());
            return 0;
        }

        if (items == null || items.isEmpty()) {
            log.warn("localbokjiro API 응답 데이터가 비어 있어 동기화를 건너뜁니다.");
            return 0;
        }

        localBokjiroRepository.deactivateAll();

        int count = 0;
        for (LocalBokjiroListResponseDTO.LocalWelfareItem item : items) {
            if (item.getServId() == null || item.getServNm() == null) {
                continue;
            }

            Integer inqNum = null;
            if (item.getInqNum() != null) {
                try {
                    inqNum = Integer.parseInt(item.getInqNum().trim());
                } catch (NumberFormatException ignored) {}
            }

            LocalBokjiroEntity entity = LocalBokjiroEntity.builder()
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
                    .ctpvNm(item.getCtpvNm())
                    .sggNm(item.getSggNm())
                    .active(true)
                    .syncedAt(LocalDateTime.now())
                    .build();

            localBokjiroRepository.upsert(entity);
            count++;
        }

        return count;
    }
}
