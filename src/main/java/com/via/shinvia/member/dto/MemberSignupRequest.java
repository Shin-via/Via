package com.via.shinvia.member.dto;

import java.time.LocalDate;

public class MemberSignupRequest {
    private String loginEmail;
    private String password;
    private String passwordConfirm;
    private String userName;
    private String phoneNumber;
    private LocalDate birthDate;
}
