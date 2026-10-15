package com.blogdock.auth;

import jakarta.validation.constraints.NotBlank;

/** AUTH-01 로그인. 형식 검사는 하지 않고, 틀리면 아이디·비밀번호 중 무엇이 틀렸는지 말하지 않는다. */
public record LoginRequest(
        @NotBlank(message = "아이디를 입력해 주세요")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요")
        String password) {
}
