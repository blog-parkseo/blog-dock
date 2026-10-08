package com.blogdock.auth;

import java.io.Serializable;

/** 세션에 저장하는 "지금 로그인한 사람" 정보. */
public record LoginMember(Long id, String nickname, String role) implements Serializable {
}
