package com.via.shinvia.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendRequestDto (@NotBlank @Email String email) {

}
