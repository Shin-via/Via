package com.via.shinvia.policy.loan.service;

import com.via.shinvia.policy.loan.dto.api.LoanProductApiItem;
import com.via.shinvia.policy.loan.entity.PolicySupportProgram;
import com.via.shinvia.policy.loan.mapper.PolicySupportProgramMapper;
import com.via.shinvia.policy.loan.repository.PolicySupportProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySupportSaveService {

    private final PolicySupportProgramMapper mapper;

    private final PolicySupportProgramRepository repository;

    @Transactional
    public int saveAll(
            List<LoanProductApiItem> apiItems
    ) {
        int processedCount = 0;
        int insertCount = 0;
        int updateCount = 0;

        for (LoanProductApiItem item : apiItems) {

            if (item.getSeq() == null
                    || item.getSeq().isBlank()) {
                continue;
            }

            PolicySupportProgram entity =
                    repository
                            .findByExternalSeq(
                                    item.getSeq()
                            )
                            .orElse(null);

            if (entity == null) {

                repository.insert(
                        mapper.toEntity(item)
                );

                insertCount++;

            } else {

                mapper.updateEntity(
                        entity,
                        item
                );

                repository.update(entity);

                updateCount++;
            }

            processedCount++;
        }

        log.info(
                "정책상품 저장 완료 - 전체={}, 신규={}, 수정={}",
                processedCount,
                insertCount,
                updateCount
        );

        return processedCount;
    }
}
