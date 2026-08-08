package com.via.shinvia.security;

import com.via.shinvia.login.security.CustomUserDetails;
import com.via.shinvia.oauth2.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    public Long getUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("로그인 필요");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        if (principal instanceof CustomOAuth2User oAuth2User) {
            return oAuth2User.getUserId();
        }
        throw new IllegalStateException(
                "지원하지 않는 인증 객체입니다: "
                        + principal.getClass().getName()
        );
    }
}
