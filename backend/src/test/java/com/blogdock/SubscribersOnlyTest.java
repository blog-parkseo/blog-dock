package com.blogdock;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** POST-12 구독자 공개 글: 손님, 구독 안 한 회원, 구독자, 주인 네 경우. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:subonly;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class SubscribersOnlyTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    MockHttpSession login(String nickname) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/dev-login").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}")).andExpect(status().isOk());
        return session;
    }

    ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
        return mvc.perform(req.session(s).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    JsonNode publish(MockHttpSession s, String title, String visibility) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("title", title);
        b.put("contentMarkdown", title + " 본문");
        b.put("contentHtml", "<p>" + title + " 본문</p>");
        b.put("publish", true);
        b.put("visibility", visibility);
        b.put("tags", java.util.List.of("비밀태그"));
        String body = send(post("/api/posts"), s, b).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void subscribers_only_post_by_viewer() throws Exception {
        MockHttpSession owner = login("so-owner");
        send(post("/api/blogs"), owner, Map.of("address", "so-blog", "name", "구독자 블로그"))
                .andExpect(status().isCreated());
        publish(owner, "공개 글", "PUBLIC");
        JsonNode secret = publish(owner, "구독자 글", "SUBSCRIBERS");
        String detail = "/api/blogs/so-blog/posts/" + secret.get("slug").asText();
        long postId = secret.get("id").asLong();

        MockHttpSession stranger = login("so-stranger");
        MockHttpSession fan = login("so-fan");
        mvc.perform(post("/api/blogs/so-blog/subscription").session(fan)).andExpect(status().isOk());

        // 손님과 구독 안 한 회원: 목록·검색·사이드바·태그·홈에서 빠지고, 주소로 열면 잠긴 화면
        for (MockHttpSession s : new MockHttpSession[] {new MockHttpSession(), stranger}) {
            mvc.perform(get("/api/blogs/so-blog/posts").session(s))
                    .andExpect(jsonPath("$.items", hasSize(1))).andExpect(jsonPath("$.totalElements").value(1));
            mvc.perform(get("/api/blogs/so-blog/posts").param("q", "구독자").session(s))
                    .andExpect(jsonPath("$.items", hasSize(0)));
            mvc.perform(get("/api/blogs/so-blog/sidebar").session(s)).andExpect(jsonPath("$.totalCount").value(1));
            mvc.perform(get("/api/blogs/so-blog/tags").session(s)).andExpect(jsonPath("$[0].count").value(1));
            mvc.perform(get(detail).session(s)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(true))
                    .andExpect(jsonPath("$.title").value("구독자 글"))
                    .andExpect(jsonPath("$.contentHtml").value(""))
                    .andExpect(jsonPath("$.tags", hasSize(0)));
            mvc.perform(get("/api/posts/" + postId + "/comments").session(s)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/home/posts")).andExpect(jsonPath("$.items", hasSize(1)));
        mvc.perform(post("/api/posts/" + postId + "/like").session(stranger)).andExpect(status().isNotFound());
        send(post("/api/posts/" + postId + "/comments"), stranger, Map.of("content", "몰래"))
                .andExpect(status().isNotFound());

        // 구독자: 다 보이고 피드에도 뜨고 댓글·공감도 된다
        mvc.perform(get("/api/blogs/so-blog/posts").session(fan)).andExpect(jsonPath("$.items", hasSize(2)));
        mvc.perform(get("/api/blogs/so-blog/sidebar").session(fan)).andExpect(jsonPath("$.totalCount").value(2));
        mvc.perform(get("/api/blogs/so-blog/tags").session(fan)).andExpect(jsonPath("$[0].count").value(2));
        mvc.perform(get(detail).session(fan)).andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.contentHtml").value("<p>구독자 글 본문</p>"))
                .andExpect(jsonPath("$.prev.title").value("공개 글"));
        mvc.perform(get("/api/feed").session(fan)).andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].visibility").value("SUBSCRIBERS"));
        mvc.perform(post("/api/posts/" + postId + "/like").session(fan)).andExpect(status().isOk());
        send(post("/api/posts/" + postId + "/comments"), fan, Map.of("content", "잘 봤어요"))
                .andExpect(status().isCreated());

        // 주인: 다 보이고, 관리 페이지에 구독자 공개 탭과 개수가 있다
        mvc.perform(get(detail).session(owner)).andExpect(jsonPath("$.locked").value(false));
        mvc.perform(get("/api/me/posts").param("status", "SUBSCRIBERS").session(owner))
                .andExpect(jsonPath("$.items", hasSize(1)));
        mvc.perform(get("/api/me/posts/counts").session(owner))
                .andExpect(jsonPath("$.publicCount").value(1)).andExpect(jsonPath("$.subscribersCount").value(1));

        // 구독을 끊으면 바로 다시 잠긴다
        mvc.perform(delete("/api/blogs/so-blog/subscription").session(fan)).andExpect(status().isOk());
        mvc.perform(get(detail).session(fan)).andExpect(jsonPath("$.locked").value(true));
        mvc.perform(get("/api/feed").session(fan)).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void private_post_stays_404() throws Exception {
        MockHttpSession owner = login("so-owner2");
        send(post("/api/blogs"), owner, Map.of("address", "so-blog2", "name", "블로그"))
                .andExpect(status().isCreated());
        JsonNode hidden = publish(owner, "비공개 글", "PRIVATE");
        MockHttpSession fan = login("so-fan2");
        mvc.perform(post("/api/blogs/so-blog2/subscription").session(fan)).andExpect(status().isOk());
        mvc.perform(get("/api/blogs/so-blog2/posts/" + hidden.get("slug").asText()).session(fan))
                .andExpect(status().isNotFound());
    }
}
