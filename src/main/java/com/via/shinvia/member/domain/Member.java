package com.via.shinvia.member.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter @ToString(exclude="passwordHash")
public class Member {
    private Long userId;
    private String loginEmail;
    private String passwordHash;
    private String userName;
    private String phoneNumber;
    private LocalDate birthDate;
    private MemberStatus userStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MemberRole userRole;
}
