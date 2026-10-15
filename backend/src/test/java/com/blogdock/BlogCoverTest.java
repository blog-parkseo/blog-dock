package com.blogdock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 블로그 위쪽 배경: 고르면 저장되고, AUTO면 자동 색으로, 안 보내면 그대로, 없는 코드는 막는다. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cover;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class BlogCoverTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
        return mvc.perform(req.session(s).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    @Test
    void cover() throws Exception {
        MockHttpSession owner = new MockHttpSession();
        send(post("/api/auth/dev-login"), owner, Map.of("nickname", "cover-owner")).andExpect(status().isOk());
        send(post("/api/blogs"), owner, Map.of("address", "cover-blog", "name", "배경")).andExpect(status().isCreated());
        mvc.perform(get("/api/blogs/cover-blog")).andExpect(jsonPath("$.cover").doesNotExist());

        send(patch("/api/blogs/cover-blog"), owner, Map.of("name", "배경", "cover", "SUNSET"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cover").value("SUNSET"));
        // cover를 안 보내면 그대로
        send(patch("/api/blogs/cover-blog"), owner, Map.of("name", "배경 2"))
                .andExpect(jsonPath("$.cover").value("SUNSET"));
        mvc.perform(get("/api/blogs/cover-blog/sidebar")).andExpect(jsonPath("$.blog.cover").value("SUNSET"));
        send(patch("/api/blogs/cover-blog"), owner, Map.of("name", "배경", "cover", "PURPLE"))
                .andExpect(status().isBadRequest());
        send(patch("/api/blogs/cover-blog"), owner, Map.of("name", "배경", "cover", "AUTO"))
                .andExpect(jsonPath("$.cover").doesNotExist());
    }
}
