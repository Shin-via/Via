package com.via.shinvia.auth.service;

import com.via.shinvia.member.domain.Member;
import com.via.shinvia.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Override
    public UserDetails loadUserByUsername(String loginEmail) throws UsernameNotFoundException {
        Member member = memberMapper.findByLoginEmail(loginEmail);
        if (member==null){
            throw new UsernameNotFoundException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        return User.builder()
                    .username(member.getLoginEmail())
                    .password(member.getPasswordHash())
                    .roles(member.getUserRole().name())
                    .build();
    }
}
