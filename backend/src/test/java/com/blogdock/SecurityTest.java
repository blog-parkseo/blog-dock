package com.blogdock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

/** CSRF 방어: 쿠키의 토큰을 헤더로 돌려보내지 않으면 상태를 바꾸는 요청은 막힌다. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sec;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    void csrf_token_required_for_changes() throws Exception {
        mvc.perform(post("/api/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"sec\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF"));

        String token = mvc.perform(get("/api/health")).andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN").getValue();
        mvc.perform(post("/api/auth/dev-login").cookie(new Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"sec\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void providers_without_kakao_keys() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.kakao").value(false)).andExpect(jsonPath("$.dev").value(true));
    }
}
