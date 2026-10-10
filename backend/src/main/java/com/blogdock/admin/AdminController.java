package com.blogdock.admin;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN-01: /api/admin/** 는 SecurityConfig에서 ADMIN만 통과시킨다. 실제 관리 기능은 2차. */
@RestController
public class AdminController {

    @GetMapping("/api/admin/ping")
    public Map<String, String> ping() {
        return Map.of("message", "관리자 영역이에요");
    }
}
