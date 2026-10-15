package com.blogdock.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** AUTH-05. profileImageUrl은 이미지 업로드로 받은 주소, 비우면 기본 이미지. */
public record ProfileRequest(
        @NotBlank(message = "닉네임을 입력해 주세요")
        @Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요") String nickname,
        @Pattern(regexp = "^$|^/files/[A-Za-z0-9/_.-]+$", message = "프로필 이미지를 다시 올려 주세요")
        String profileImageUrl) {
}
