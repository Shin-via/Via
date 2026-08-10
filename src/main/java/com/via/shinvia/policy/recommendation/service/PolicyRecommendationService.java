package com.via.shinvia.policy.recommendation.service;

import com.via.shinvia.policy.recommendation.dto.PolicyRecommendationProfileDTO;
import com.via.shinvia.policy.recommendation.mapper.PolicyRecommendationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyRecommendationService {

    private final PolicyRecommendationMapper recommendationMapper;


    // 기존 설문 조회
    @Transactional(readOnly = true)
    public PolicyRecommendationProfileDTO getProfile(Long userId) {

        return recommendationMapper.findByUserId(userId);
    }


    // 설문 저장 또는 수정
    @Transactional
    public void saveProfile(
            PolicyRecommendationProfileDTO profile
    ) {

        recommendationMapper.upsertProfile(profile);
    }
}