package com.via.shinvia.policy.recommendation.controller;

import com.via.shinvia.policy.recommendation.profile.dto.PolicyRecommendationProfileDTO;
import com.via.shinvia.policy.recommendation.profile.service.PolicyRecommendationProfileService;
import com.via.shinvia.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy/recommendation")
@RequiredArgsConstructor
public class PolicyRecommendationApiController {

    private final PolicyRecommendationProfileService profileService;
    private final CurrentUser currentUser;

    // 현재 설문 조회
    @GetMapping
    public ResponseEntity<PolicyRecommendationProfileDTO> getProfile(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    // 설문 저장
    @PostMapping
    public ResponseEntity<Void> saveProfile(
            @Valid @RequestBody PolicyRecommendationProfileDTO request,
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        request.setUserId(userId);
        profileService.saveProfile(request);
        return ResponseEntity.ok().build();
    }
}
