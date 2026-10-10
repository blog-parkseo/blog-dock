package com.blogdock.auth;

/** blogAddress가 null이면 아직 블로그가 없다 (AUTH-04에서 사용). */
public record MeResponse(Long id, String nickname, String profileImageUrl, String role, String blogAddress) {
}
