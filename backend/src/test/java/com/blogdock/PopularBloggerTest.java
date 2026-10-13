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

import com.blogdock.subscription.PopularBloggerService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** HOME-04 인기 블로거. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:blogger;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class PopularBloggerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    PopularBloggerService bloggers;

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

    MockHttpSession owner(String nick, String address) throws Exception {
        MockHttpSession s = login(nick);
        send(post("/api/blogs"), s, Map.of("address", address, "name", nick + "의 블로그")).andExpect(status().isCreated());
        return s;
    }

    @Test
    void ranks_blogs_by_recent_likes_comments_and_new_subscribers() throws Exception {
        bloggers.refresh();
        mvc.perform(get("/api/home/bloggers")).andExpect(jsonPath("$", hasSize(0)));

        MockHttpSession a = owner("bg-a", "bg-a");
        MockHttpSession b = owner("bg-b", "bg-b");
        owner("bg-c", "bg-c"); // 공개 글이 없다
        MockHttpSession d = owner("bg-d", "bg-d");
        MockHttpSession e = owner("bg-e", "bg-e");
        MockHttpSession r1 = login("bg-r1");
        MockHttpSession r2 = login("bg-r2");

        long pa = publish(a, "에이 글");
        long pb = publish(b, "비 글");
        publish(d, "아무 반응 없는 글");
        long pe = publish(e, "이 글");

        like(r1, pa);
        like(r2, pa);
        comment(a, pa); // 블로그 주인 본인 댓글은 세지 않는다 → A = 2
        comment(r1, pb);
        mvc.perform(post("/api/blogs/bg-b/subscription").session(r1)).andExpect(status().isOk()); // B = 2, 구독자 1
        mvc.perform(post("/api/blogs/bg-c/subscription").session(r1)).andExpect(status().isOk()); // C는 공개 글이 없어 빠짐
        like(r2, pe); // E = 1

        bloggers.refresh();
        mvc.perform(get("/api/home/bloggers").session(r1))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].address").value("bg-b")) // A와 동점, 전체 구독자가 더 많다
                .andExpect(jsonPath("$[0].subscribed").value(true))
                .andExpect(jsonPath("$[0].subscriberCount").value(1))
                .andExpect(jsonPath("$[1].address").value("bg-a"))
                .andExpect(jsonPath("$[1].subscribed").value(false))
                .andExpect(jsonPath("$[2].address").value("bg-e"));
        // 비회원도 볼 수 있고, 블로그 주인에게는 자기 카드가 주인으로 표시된다
        mvc.perform(get("/api/home/bloggers")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/home/bloggers").session(b)).andExpect(jsonPath("$[0].isOwner").value(true));
    }
}
