package com.via.shinvia.member.mapper;

import com.via.shinvia.member.domain.Member;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

@Mapper
public interface MemberMapper {
    int insertMember(Member member);
    boolean existsByLoginEmail(@Param("loginEmail") String loginEmail);
    Member findByLoginEmail(@Param("loginEmail") String loginEmail);
}
