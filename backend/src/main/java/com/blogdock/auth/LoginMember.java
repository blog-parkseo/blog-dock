package com.blogdock.auth;

import java.io.Serializable;

import com.blogdock.common.ApiException;

/** 세션에 저장하는 "지금 로그인한 사람" 정보. */
public record LoginMember(Long id, String nickname, String role) implements Serializable {

    /** 비회원이면 null. */
    public static Long idOf(LoginMember login) {
        return login == null ? null : login.id();
    }

    /** 로그인하지 않았으면 401. */
    public static Long require(LoginMember login) {
        if (login == null) {
            throw ApiException.unauthorized();
        }
        return login.id();
    }
}
