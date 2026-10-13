package com.blogdock;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
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

import com.blogdock.post.PopularPostService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** HOME-02 인기 글. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:popular;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class PopularPostTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    PopularPostService popular;

    MockHttpSession login(String nickname) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/dev-login").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}")).andExpect(status().isOk());
        return session;
    }

    ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
        return mvc.perform(req.session(s).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    long publish(MockHttpSession s, String title) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("title", title);
        b.put("contentMarkdown", "<p>" + title + "</p>");
        b.put("contentHtml", "<p>" + title + "</p>");
        b.put("publish", true);
        b.put("visibility", "PUBLIC");
        String body = send(post("/api/posts"), s, b).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    void like(MockHttpSession s, long postId) throws Exception {
        mvc.perform(post("/api/posts/" + postId + "/like").session(s)).andExpect(status().isOk());
    }

    void comment(MockHttpSession s, long postId) throws Exception {
        send(post("/api/posts/" + postId + "/comments"), s, Map.of("content", "좋아요")).andExpect(status().isCreated());
    }

    @Test
    void ranks_by_recent_likes_plus_comments_without_owner_comments() throws Exception {
        MockHttpSession owner = login("pop-owner");
        send(post("/api/blogs"), owner, Map.of("address", "pop-blog", "name", "인기 블로그")).andExpect(status().isCreated());
        MockHttpSession r1 = login("pop-r1");
        MockHttpSession r2 = login("pop-r2");

        popular.refresh();
        mvc.perform(get("/api/home/popular")).andExpect(jsonPath("$", hasSize(0)));

        publish(owner, "조용한 글");
        long self = publish(owner, "혼자 댓글 단 글");
        long liked = publish(owner, "공감 두 개");
        long mixed = publish(owner, "공감 하나 댓글 둘");
        long hidden = publish(owner, "나중에 비공개");

        for (int i = 0; i < 5; i++) {
            comment(owner, self); // 글쓴이 본인 댓글은 세지 않는다
        }
        like(r1, liked);
        like(r2, liked);
        like(r1, mixed);
        comment(r1, mixed);
        comment(r2, mixed);
        like(r1, hidden);

        popular.refresh();
        mvc.perform(get("/api/home/popular"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("공감 하나 댓글 둘"))
                .andExpect(jsonPath("$[1].title").value("공감 두 개"))
                .andExpect(jsonPath("$[2].title").value("나중에 비공개"))
                .andExpect(jsonPath("$[0].blog.address").value("pop-blog"));

        // 계산한 뒤 비공개로 바꾸면 다음 계산 전에도 바로 빠진다
        send(patch("/api/posts/" + hidden + "/visibility"), owner, Map.of("visibility", "PRIVATE"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/home/popular")).andExpect(jsonPath("$", hasSize(2)));
    }
}
