package com.via.shinvia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // 현재 계좌 동기화 API 테스트에만 CSRF 제외
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/accounts/sync"
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        // 로그인 구현 전 Mock 연동 테스트용
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/accounts/sync"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        // 나머지 요청은 일단 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}