package com.blogdock.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevLoginRequest(
        @NotBlank(message = "닉네임을 입력해 주세요")
        @Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요")
        String nickname) {
}
