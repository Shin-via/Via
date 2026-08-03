package com.via.shinvia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 로그인 세션이 아직 없어서(TODO: 회원인증 미구현), 기본 Spring Security 로그인 화면 없이
 * 모든 요청을 허용한다. 회원인증이 생기면 /api/** 등 엔드포인트별로 인증을 다시 걸어야 한다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
