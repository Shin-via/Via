package com.via.shinvia.policy.recommendation.controller;

import com.via.shinvia.policy.recommendation.dto.PolicyRecommendationProfileDTO;
import com.via.shinvia.policy.recommendation.service.PolicyRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policy/recommendation")
@RequiredArgsConstructor
public class PolicyRecommendationApiController {

    private final PolicyRecommendationService recommendationService;


    // ============================
    // 현재 설문 조회
    // ============================
    @GetMapping
    public ResponseEntity<PolicyRecommendationProfileDTO> getProfile() {

        /*
         * TODO
         * 회원 연동 완료 후 Authentication에서 userId 조회
         *
         * 현재는 개발 테스트용 회원 1번 사용
         */
        Long userId = 1L;

        PolicyRecommendationProfileDTO profile =
                recommendationService.getProfile(userId);

        return ResponseEntity.ok(profile);
    }


    // ============================
    // 설문 저장
    // ============================
    @PostMapping
    public ResponseEntity<Void> saveProfile(
            @RequestBody PolicyRecommendationProfileDTO request
    ) {

        /*
         * TODO
         * 회원 연동 완료 후 Authentication에서 userId 조회
         */
        Long userId = 1L;

        // 프론트에서 userId를 받지 않고 서버에서 지정
        request.setUserId(userId);

        recommendationService.saveProfile(request);

        return ResponseEntity.ok().build();
    }
}