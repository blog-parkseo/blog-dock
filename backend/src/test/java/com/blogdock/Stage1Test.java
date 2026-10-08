package com.blogdock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** 1단계 기능 8개를 API로 한 바퀴 확인한다. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class Stage1Test {

    @Autowired
    MockMvc mvc;

    MockHttpSession login(String nickname) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/dev-login").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(nickname));
        return session;
    }

    @Test
    void health_and_errors() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
        mvc.perform(get("/api/blogs/nobody")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post("/api/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("nickname"));
    }

    @Test
    void login_me_logout() throws Exception {
        MockHttpSession s = login("newbie");
        mvc.perform(get("/api/me").session(s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.blogAddress").doesNotExist());
        mvc.perform(post("/api/auth/logout").session(s)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(s)).andExpect(status().isUnauthorized());
    }

    @Test
    void admin_area() throws Exception {
        mvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/ping").session(login("bori"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/ping").session(login("admin"))).andExpect(status().isOk());
    }

    @Test
    void open_and_edit_blog() throws Exception {
        MockHttpSession owner = login("bori");
        mvc.perform(get("/api/blogs/address-check").param("address", "Bad")).andExpect(jsonPath("$.reason").value("FORMAT"));
        mvc.perform(get("/api/blogs/address-check").param("address", "admin")).andExpect(jsonPath("$.reason").value("RESERVED"));
        mvc.perform(get("/api/blogs/address-check").param("address", "my-dog")).andExpect(jsonPath("$.available").value(true));

        mvc.perform(post("/api/blogs").session(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"-dog\",\"name\":\"보리네\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields[0].field").value("address"));
        mvc.perform(post("/api/blogs").session(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"my-dog\",\"name\":\"보리네\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.isOwner").value(true));
        mvc.perform(post("/api/blogs").session(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"my-dog2\",\"name\":\"두번째\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BLOG_ALREADY_EXISTS"));
        mvc.perform(get("/api/blogs/address-check").param("address", "my-dog")).andExpect(jsonPath("$.reason").value("TAKEN"));
        mvc.perform(get("/api/me").session(owner)).andExpect(jsonPath("$.blogAddress").value("my-dog"));

        mvc.perform(get("/api/blogs/my-dog")).andExpect(status().isOk()).andExpect(jsonPath("$.isOwner").value(false));
        mvc.perform(patch("/api/blogs/my-dog").session(login("other")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"뺏기\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/blogs/my-dog").session(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"보리네 산책일기\",\"description\":\"산책 기록\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("보리네 산책일기"));
        mvc.perform(post("/api/blogs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"anon\",\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
