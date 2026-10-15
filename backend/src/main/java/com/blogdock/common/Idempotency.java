package com.blogdock.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 글 발행·댓글 등록을 연달아 눌러도 한 번만 처리한다 (POST-01, CMT-01).
 * 화면이 버튼을 누를 때마다 같은 요청 키(Idempotency-Key 헤더)를 보내면,
 * 두 번째부터는 처음 결과를 그대로 돌려준다.
 */
@Component
public class Idempotency {

    public static final String HEADER = "Idempotency-Key";

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public Idempotency(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<?> run(Long userId, String key, Object request, HttpStatus status, Supplier<?> action) {
        if (key == null || key.isBlank()) {
            return ResponseEntity.status(status).body(action.get());
        }
        if (key.length() > 36) {
            throw ApiException.badRequest("BAD_IDEMPOTENCY_KEY", "요청 키가 너무 길어요");
        }
        String hash = sha256(toJson(request));
        try {
            // 먼저 자리를 잡는다. 같은 키가 이미 있으면 DB 기본 키가 막아 준다
            jdbc.update("insert into idempotency_record (user_id, idem_key, request_hash) values (?, ?, ?)",
                    userId, key, hash);
        } catch (DuplicateKeyException e) {
            return replay(userId, key, hash);
        }
        Object result;
        try {
            result = action.get();
        } catch (RuntimeException e) {
            // 실패하면 자리를 비워서 다시 시도할 수 있게 한다
            jdbc.update("delete from idempotency_record where user_id = ? and idem_key = ?", userId, key);
            throw e;
        }
        jdbc.update("update idempotency_record set response_status = ?, response_body = ? where user_id = ? and idem_key = ?",
                status.value(), toJson(result), userId, key);
        return ResponseEntity.status(status).body(result);
    }

    private ResponseEntity<?> replay(Long userId, String key, String hash) {
        List<Object[]> rows = jdbc.query(
                "select request_hash, response_status, response_body from idempotency_record where user_id = ? and idem_key = ?",
                (rs, i) -> new Object[] {rs.getString(1), (Integer) rs.getObject(2), rs.getString(3)},
                userId, key);
        if (rows.isEmpty()) {
            throw ApiException.conflict("IN_PROGRESS", "처리 중이에요. 잠시 후 다시 확인해 주세요");
        }
        Object[] row = rows.get(0);
        if (!hash.equals(row[0])) {
            throw ApiException.conflict("IDEMPOTENCY_MISMATCH", "같은 요청 키로 다른 내용을 보냈어요");
        }
        if (row[1] == null) {
            throw ApiException.conflict("IN_PROGRESS", "처리 중이에요. 잠시 후 다시 확인해 주세요");
        }
        try {
            JsonNode body = objectMapper.readTree((String) row[2]);
            return ResponseEntity.status((Integer) row[1]).body(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 하루 지난 기록은 지운다. */
    @Scheduled(fixedDelay = 1, timeUnit = java.util.concurrent.TimeUnit.HOURS)
    public void cleanUp() {
        jdbc.update("delete from idempotency_record where created_at < ?",
                java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
