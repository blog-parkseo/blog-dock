package com.blogdock.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** AUTH-01 회원가입. 규칙은 명세서 공통 정책 "아이디·비밀번호"와 같다. */
public record SignupRequest(
        @NotBlank(message = "아이디를 입력해 주세요")
        @Pattern(regexp = "^[a-z][a-z0-9]{3,19}$",
                message = "아이디는 영문 소문자로 시작하고 영문 소문자·숫자로 4~20자예요")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).*$", message = "비밀번호에 영문과 숫자를 모두 넣어 주세요")
        String password,

        @NotBlank(message = "닉네임을 입력해 주세요")
        @Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요")
        String nickname) {
}
