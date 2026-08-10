package com.via.shinvia.policy.recommendation.mapper;

import com.via.shinvia.policy.recommendation.dto.PolicyRecommendationProfileDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PolicyRecommendationMapper {

    // 회원의 기존 설문 조회
    PolicyRecommendationProfileDTO findByUserId(
            @Param("userId") Long userId
    );

    // 최초 저장 또는 기존 설문 수정
    int upsertProfile(
            PolicyRecommendationProfileDTO profile
    );
}