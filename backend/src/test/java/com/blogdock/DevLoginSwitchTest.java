package com.blogdock;

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
import org.springframework.test.web.servlet.MockMvc;

/** 운영처럼 admin 로그인을 막아 두면: 닉네임 로그인은 되고 admin으로는 못 들어간다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:devlogin;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "blogdock.dev-login.admin=false"})
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class DevLoginSwitchTest {

    @Autowired
    MockMvc mvc;

    @Test
    void nickname_login_works_but_admin_is_blocked() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.dev").value(true));
        mvc.perform(post("/api/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"서현\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("서현"));
        mvc.perform(post("/api/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\" admin \"}"))
                .andExpect(status().isForbidden());
    }
}
