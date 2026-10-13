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

import com.fasterxml.jackson.databind.ObjectMapper;

/** SUB-01 구독·해제, SUB-02 구독 목록, SUB-03 구독 피드. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sub;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class SubscriptionTest {

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

    MockHttpSession blogOwner(String nickname, String address) throws Exception {
        MockHttpSession s = login(nickname);
        send(post("/api/blogs"), s, Map.of("address", address, "name", nickname + "의 블로그"))
                .andExpect(status().isCreated());
        return s;
    }

    ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
        return mvc.perform(req.session(s).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    void publish(MockHttpSession s, String title, String visibility) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("title", title);
        b.put("contentMarkdown", "<p>" + title + "</p>");
        b.put("contentHtml", "<p>" + title + "</p>");
        b.put("publish", true);
        b.put("visibility", visibility);
        send(post("/api/posts"), s, b).andExpect(status().isCreated());
    }

    @Test
    void subscribe_twice_counts_once_and_unsubscribe() throws Exception {
        blogOwner("sub-owner", "sub-blog");
        MockHttpSession reader = login("sub-reader");

        mvc.perform(post("/api/blogs/sub-blog/subscription").session(reader)).andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true)).andExpect(jsonPath("$.subscriberCount").value(1));
        mvc.perform(post("/api/blogs/sub-blog/subscription").session(reader))
                .andExpect(jsonPath("$.subscriberCount").value(1));

        mvc.perform(get("/api/blogs/sub-blog/sidebar").session(reader))
                .andExpect(jsonPath("$.blog.subscribed").value(true))
                .andExpect(jsonPath("$.blog.subscriberCount").value(1));
        mvc.perform(get("/api/blogs/sub-blog"))
                .andExpect(jsonPath("$.subscribed").value(false))
                .andExpect(jsonPath("$.subscriberCount").value(1));
        mvc.perform(get("/api/me/subscriptions").session(reader))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].address").value("sub-blog"));

        mvc.perform(delete("/api/blogs/sub-blog/subscription").session(reader))
                .andExpect(jsonPath("$.subscribed").value(false)).andExpect(jsonPath("$.subscriberCount").value(0));
        mvc.perform(delete("/api/blogs/sub-blog/subscription").session(reader)).andExpect(status().isOk());
        mvc.perform(get("/api/me/subscriptions").session(reader)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void own_blog_guest_and_missing_blog_are_refused() throws Exception {
        MockHttpSession owner = blogOwner("own-owner", "own-blog");
        mvc.perform(post("/api/blogs/own-blog/subscription").session(owner)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OWN_BLOG"));
        mvc.perform(post("/api/blogs/own-blog/subscription").session(new MockHttpSession()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/blogs/no-such-blog/subscription").session(login("own-reader")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/feed").session(new MockHttpSession())).andExpect(status().isUnauthorized());
    }

    @Test
    void feed_shows_only_public_posts_of_subscribed_blogs_newest_first() throws Exception {
        MockHttpSession a = blogOwner("feed-a", "feed-a");
        MockHttpSession b = blogOwner("feed-b", "feed-b");
        MockHttpSession c = blogOwner("feed-c", "feed-c");
        MockHttpSession reader = login("feed-reader");
        mvc.perform(post("/api/blogs/feed-a/subscription").session(reader)).andExpect(status().isOk());
        mvc.perform(post("/api/blogs/feed-b/subscription").session(reader)).andExpect(status().isOk());

        mvc.perform(get("/api/feed").session(reader)).andExpect(jsonPath("$.items", hasSize(0)));

        publish(a, "에이 첫 글", "PUBLIC");
        publish(c, "구독 안 한 블로그 글", "PUBLIC");
        publish(b, "비 비공개 글", "PRIVATE");
        for (int i = 1; i <= 10; i++) {
            publish(b, "비 글 " + i, "PUBLIC");
        }

        mvc.perform(get("/api/feed").session(reader))
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.items[0].title").value("비 글 10"))
                .andExpect(jsonPath("$.items[0].blog.address").value("feed-b"))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
        String cursor = json.readTree(mvc.perform(get("/api/feed").session(reader)).andReturn()
                .getResponse().getContentAsString()).get("nextCursor").asText();
        mvc.perform(get("/api/feed").param("cursor", cursor).session(reader))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("에이 첫 글"))
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }
}
