package com.blogdock.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 비밀번호를 하나씩 넣어 보는 공격을 막는다. 같은 아이디로 5분 안에 5번 틀리면 5분 동안 로그인을 막는다.
 * 서버 메모리에만 기억하므로 서버를 다시 켜면 초기화된다.
 */
@Component
public class LoginAttempts {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(5);

    private record Failures(int count, Instant since) {
    }

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String loginId) {
        Failures f = failures.get(loginId);
        if (f == null) {
            return false;
        }
        if (f.since().plus(WINDOW).isBefore(Instant.now())) {
            failures.remove(loginId, f);
            return false;
        }
        return f.count() >= MAX_FAILURES;
    }

    public void fail(String loginId) {
        Instant now = Instant.now();
        if (failures.size() > 10_000) {
            failures.values().removeIf(f -> f.since().plus(WINDOW).isBefore(now)); // 오래된 기록 정리
        }
        failures.compute(loginId, (k, f) ->
                f == null || f.since().plus(WINDOW).isBefore(now) ? new Failures(1, now) : new Failures(f.count() + 1, f.since()));
    }

    public void succeed(String loginId) {
        failures.remove(loginId);
    }
}
