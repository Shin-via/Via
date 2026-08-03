package com.via.shinvia.member.service;

import com.via.shinvia.member.domain.Member;
import com.via.shinvia.member.domain.MemberRole;
import com.via.shinvia.member.domain.MemberStatus;
import com.via.shinvia.member.dto.MemberSignupRequestDto;
import com.via.shinvia.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Member member = new Member();
        member.setLoginEmail(verifiedEmail);
        member.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        member.setUserName(request.getUserName());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setBirthDate(request.getBirthDate());
        member.setUserStatus(MemberStatus.ACTIVE);
        member.setUserRole(MemberRole.USER);

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
}
