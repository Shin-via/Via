package com.via.shinvia.oauth2.domain;

import java.time.LocalDateTime;

public class SocialUser {
    private Long socialUserId;
    private Long userId;
    private SocialProvider provider;
    private String providerUserId;
    private String providerEmail;
    private LocalDateTime createdAt;
}
