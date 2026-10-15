package com.blogdock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

/** 롤링 배포: 로그인 세션이 DB(SPRING_SESSION)에 저장돼서 다른 블로그 서버도 같은 로그인을 본다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:session;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "blogdock.upload-dir=target/test-uploads",
        "spring.autoconfigure.exclude="})
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class SessionStoreTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void login_session_is_saved_in_database() throws Exception {
        Cookie cookie = mvc.perform(post("/api/auth/dev-login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"세션확인\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("JSESSIONID");
        assertThat(cookie).isNotNull();

        // 서버 메모리가 아니라 쿠키 하나로 로그인이 이어진다
        mvc.perform(get("/api/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("세션확인"));
        assertThat(jdbc.queryForObject("select count(*) from SPRING_SESSION", Integer.class)).isPositive();

        // 로그아웃하면 세션이 사라진다
        mvc.perform(post("/api/auth/logout").cookie(cookie));
        mvc.perform(get("/api/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }
}
