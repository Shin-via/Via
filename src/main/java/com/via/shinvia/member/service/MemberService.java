package com.via.shinvia.member.service;

import com.via.shinvia.member.domain.Member;
import com.via.shinvia.member.domain.MemberStatus;
import com.via.shinvia.member.dto.MemberSignupRequestDto;
import com.via.shinvia.member.mapper.MemberMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(
            MemberSignupRequestDto request,
            String verifiedEmail
    ) {
        String loginEmail = normalizeEmail(request.getLoginEmail());

        validateVerifiedEmail(loginEmail, verifiedEmail);
        validateDuplicateEmail(loginEmail);

        Member member = new Member();
        member.setLoginEmail(loginEmail);
        member.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        member.setUserName(request.getUserName());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setBirthDate(request.getBirthDate());
        member.setUserStatus(MemberStatus.ACTIVE);

        try{
            int insertedCount = memberMapper.insertMember(member);
            if(insertedCount != 1) {
                throw new IllegalStateException("회원 저장에 실패했습니다.");
            }
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.", e);
        }
        return member.getUserId();
    }

    private void validateVerifiedEmail(String loginEmail, String verifiedEmail) {
        if(verifiedEmail==null || !loginEmail.equals(normalizeEmail(verifiedEmail))) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }
    }

    private void validateDuplicateEmail(String loginEmail) {
        if (memberMapper.existsByLoginEmail(loginEmail)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()){
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
