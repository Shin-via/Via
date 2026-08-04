package com.via.shinvia.oauth2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class SocialSignupRequestDto {

    @NotBlank(message="이름은 필수입니다.")
    private String userName;

    @NotBlank
    private String phoneNumber;

    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;
}