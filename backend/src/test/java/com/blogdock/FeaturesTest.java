package com.blogdock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 2~4단계 기능을 API로 확인한다. 테스트마다 다른 닉네임·블로그를 써서 서로 섞이지 않게 한다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "blogdock.upload-dir=target/test-uploads"})
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class FeaturesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ---------- 도우미 ----------

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

    ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                       MockHttpSession s, Object body) throws Exception {
        if (s != null) {
            req.session(s);
        }
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    Map<String, Object> postBody(String title, String html) {
        Map<String, Object> b = new HashMap<>();
        b.put("title", title);
        b.put("contentMarkdown", html);
        b.put("contentHtml", html);
        b.put("publish", true);
        b.put("visibility", "PUBLIC");
        return b;
    }

    long publish(MockHttpSession s, Map<String, Object> body) throws Exception {
        return read(send(post("/api/posts"), s, body).andExpect(status().isCreated())).get("id").asLong();
    }

    long publish(MockHttpSession s, String title) throws Exception {
        return publish(s, postBody(title, "<p>" + title + " 본문</p>"));
    }

    // ---------- 테스트 ----------

    @Test
    void categories_cat01_cat04() throws Exception {
        MockHttpSession owner = blogOwner("cat-owner", "cat-blog");
        long life = read(send(post("/api/blogs/cat-blog/categories"), owner, Map.of("name", "일상"))
                .andExpect(status().isCreated())).get("id").asLong();
        long dev = read(send(post("/api/blogs/cat-blog/categories"), owner, Map.of("name", "개발"))
                .andExpect(status().isCreated())).get("id").asLong();
        send(post("/api/blogs/cat-blog/categories"), owner, Map.of("name", "일상"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields[0].field").value("name"));
        send(post("/api/blogs/cat-blog/categories"), owner, Map.of("name", "미분류")).andExpect(status().isBadRequest());
        send(post("/api/blogs/cat-blog/categories"), owner, Map.of("name", "123456789012345678901"))
                .andExpect(status().isBadRequest());
        send(post("/api/blogs/cat-blog/categories"), login("cat-stranger"), Map.of("name", "남의것"))
                .andExpect(status().isForbidden());

        // 순서 바꾸기 → 사이드바 순서
        send(put("/api/blogs/cat-blog/categories/order"), owner, Map.of("ids", List.of(dev, life))).andExpect(status().isOk());
        mvc.perform(get("/api/blogs/cat-blog/sidebar"))
                .andExpect(jsonPath("$.categories[0].name").value("개발"))
                .andExpect(jsonPath("$.categories[1].name").value("일상"));

        // 글이 있는 카테고리를 지우면 글은 미분류로
        Map<String, Object> body = postBody("카테고리 글", "<p>본문</p>");
        body.put("categoryId", life);
        publish(owner, body);
        mvc.perform(get("/api/blogs/cat-blog/sidebar"))
                .andExpect(jsonPath("$.categories[1].count").value(1))
                .andExpect(jsonPath("$.uncategorizedCount").value(0));
        send(patch("/api/categories/" + life), owner, Map.of("name", "하루")).andExpect(jsonPath("$.name").value("하루"));
        mvc.perform(delete("/api/categories/" + life).session(login("cat-stranger"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/categories/" + life).session(owner)).andExpect(status().isNoContent());
        mvc.perform(get("/api/blogs/cat-blog/sidebar"))
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.uncategorizedCount").value(1))
                .andExpect(jsonPath("$.totalCount").value(1));
        mvc.perform(get("/api/blogs/cat-blog/posts").param("category", "none"))
                .andExpect(jsonPath("$.items[0].title").value("카테고리 글"));
        mvc.perform(get("/api/blogs/cat-blog/posts").param("category", String.valueOf(life))).andExpect(status().isNotFound());
    }

    @Test
    void publish_rules_post01_post02_post04() throws Exception {
        MockHttpSession owner = blogOwner("pub-owner", "pub-blog");
        // 제목 → 본문 순서로 첫 문제만
        send(post("/api/posts"), owner, postBody(" ", "")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields", hasSize(1))).andExpect(jsonPath("$.fields[0].field").value("title"));
        send(post("/api/posts"), owner, postBody("제목", "<p> </p>")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("content"));
        send(post("/api/posts"), owner, postBody("가".repeat(101), "<p>x</p>"))
                .andExpect(jsonPath("$.fields[0].field").value("title"));
        // 블로그 없는 회원
        send(post("/api/posts"), login("pub-noblog"), postBody("제목", "<p>x</p>"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BLOG_REQUIRED"));

        Map<String, Object> body = postBody("첫 글", "<p>안녕 <b>굵게</b></p>");
        body.put("tags", List.of("일상", "#산책", "일상", " "));
        long id = publish(owner, body);
        JsonNode d = read(mvc.perform(get("/api/blogs/pub-blog/posts/" + id)).andExpect(status().isOk()));
        assertThat(d.get("tags").toString()).isEqualTo("[\"일상\",\"산책\"]");
        assertThat(d.get("isOwner").asBoolean()).isFalse();
        String publishedAt = d.get("publishedAt").asText();

        // 수정해도 주소·발행일 그대로, 남은 403
        Map<String, Object> edit = postBody("고친 글", "<p>고침</p>");
        send(put("/api/posts/" + id), login("pub-stranger"), edit).andExpect(status().isForbidden());
        send(put("/api/posts/" + id), owner, edit).andExpect(status().isOk()).andExpect(jsonPath("$.slug").value(String.valueOf(id)));
        mvc.perform(get("/api/blogs/pub-blog/posts/" + id))
                .andExpect(jsonPath("$.title").value("고친 글"))
                .andExpect(jsonPath("$.publishedAt").value(publishedAt));

        // 없는 글·다른 블로그 주소로 열면 404
        mvc.perform(get("/api/blogs/pub-blog/posts/999999")).andExpect(status().isNotFound());
        blogOwner("pub-other", "pub-other");
        mvc.perform(get("/api/blogs/pub-other/posts/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/" + id + "/edit").session(login("pub-stranger"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/posts/" + id + "/edit").session(owner)).andExpect(jsonPath("$.contentMarkdown").value("<p>고침</p>"));
    }

    @Test
    void idempotent_publish_and_comment() throws Exception {
        MockHttpSession owner = blogOwner("idem-owner", "idem-blog");
        Map<String, Object> body = postBody("한 번만", "<p>x</p>");
        String key = "11111111-1111-1111-1111-111111111111";
        JsonNode first = read(mvc.perform(post("/api/posts").session(owner).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isCreated()));
        JsonNode second = read(mvc.perform(post("/api/posts").session(owner).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isCreated()));
        assertThat(second.get("id")).isEqualTo(first.get("id"));
        mvc.perform(get("/api/blogs/idem-blog/posts")).andExpect(jsonPath("$.totalElements").value(1));
        // 같은 키로 다른 내용은 거절
        mvc.perform(post("/api/posts").session(owner).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(postBody("다른 글", "<p>y</p>"))))
                .andExpect(status().isConflict());

        long postId = first.get("id").asLong();
        MockHttpSession reader = login("idem-reader");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/posts/" + postId + "/comments").session(reader).header("Idempotency-Key", "c-1")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"좋아요\"}")).andExpect(status().isCreated());
        }
        mvc.perform(get("/api/posts/" + postId + "/comments")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void private_and_draft_post06_post08_mng01() throws Exception {
        MockHttpSession owner = blogOwner("vis-owner", "vis-blog");
        Map<String, Object> secret = postBody("비밀 글", "<p>비밀</p>");
        secret.put("visibility", "PRIVATE");
        secret.put("tags", List.of("숨은태그"));
        long privateId = publish(owner, secret);
        publish(owner, "공개 글");

        // 비회원·다른 회원: 목록, 상세, 사이드바 수, 태그, 검색, 홈 어디에도 없다
        for (MockHttpSession viewer : new MockHttpSession[] {null, login("vis-stranger")}) {
            var list = get("/api/blogs/vis-blog/posts");
            var detail = get("/api/blogs/vis-blog/posts/" + privateId);
            var sidebar = get("/api/blogs/vis-blog/sidebar");
            var tags = get("/api/blogs/vis-blog/tags");
            var search = get("/api/blogs/vis-blog/posts").param("q", "비밀");
            if (viewer != null) {
                list.session(viewer); detail.session(viewer); sidebar.session(viewer); tags.session(viewer); search.session(viewer);
            }
            mvc.perform(list).andExpect(jsonPath("$.totalElements").value(1));
            mvc.perform(detail).andExpect(status().isNotFound());
            mvc.perform(sidebar).andExpect(jsonPath("$.totalCount").value(1));
            mvc.perform(tags).andExpect(jsonPath("$", hasSize(0)));
            mvc.perform(search).andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get("/api/posts/" + privateId + "/comments")).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/home/posts")).andExpect(content().string(not(containsString("비밀 글"))));
        // 주인은 본다
        mvc.perform(get("/api/blogs/vis-blog/posts").session(owner)).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/blogs/vis-blog/posts/" + privateId).session(owner)).andExpect(status().isOk());
        mvc.perform(get("/api/blogs/vis-blog/sidebar").session(owner)).andExpect(jsonPath("$.totalCount").value(2));

        // 발행 후 공개로 바꾸기
        send(patch("/api/posts/" + privateId + "/visibility"), owner, Map.of("visibility", "PUBLIC")).andExpect(status().isOk());
        mvc.perform(get("/api/blogs/vis-blog/posts/" + privateId)).andExpect(status().isOk());
        send(patch("/api/posts/" + privateId + "/visibility"), login("vis-stranger"), Map.of("visibility", "PRIVATE"))
                .andExpect(status().isForbidden());

        // 임시저장: 목록에 없고 내 글 관리에만, 발행하면 옮겨 간다
        Map<String, Object> draft = postBody("", "");
        draft.put("publish", false);
        long draftId = read(send(post("/api/posts"), owner, draft).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))).get("id").asLong();
        mvc.perform(get("/api/me/posts").param("status", "DRAFT").session(owner)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/blogs/vis-blog/posts").session(owner)).andExpect(jsonPath("$.totalElements").value(2));
        send(put("/api/posts/" + draftId), owner, postBody("이어 쓴 글", "<p>완성</p>"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/me/posts").param("status", "DRAFT").session(owner)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/me/posts").param("status", "PUBLIC").session(owner)).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/blogs/vis-blog/posts")).andExpect(jsonPath("$.items[0].title").value("이어 쓴 글"));
    }

    @Test
    void paging_neighbors_search_blog03_post10_srch01() throws Exception {
        MockHttpSession owner = blogOwner("page-owner", "page-blog");
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            Map<String, Object> b = postBody("글 " + i, "<p>내용 " + i + (i == 3 ? " Spring_Boot 100%" : "") + "</p>");
            if (i == 5) {
                b.put("tags", List.of("Java"));
            }
            ids.add(publish(owner, b));
        }
        mvc.perform(get("/api/blogs/page-blog/posts"))
                .andExpect(jsonPath("$.items", hasSize(10))).andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items[0].title").value("글 11"))
                .andExpect(jsonPath("$.items[0].excerpt").value("내용 11"));
        mvc.perform(get("/api/blogs/page-blog/posts").param("page", "1"))
                .andExpect(jsonPath("$.items", hasSize(1))).andExpect(jsonPath("$.items[0].title").value("글 1"));
        mvc.perform(get("/api/blogs/page-blog/posts").param("page", "9")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        // 이전·다음: 가운데 글을 비공개로 바꾸면 건너뛴다
        send(patch("/api/posts/" + ids.get(5) + "/visibility"), owner, Map.of("visibility", "PRIVATE"));
        mvc.perform(get("/api/blogs/page-blog/posts/" + ids.get(4)))
                .andExpect(jsonPath("$.prev.title").value("글 4"))
                .andExpect(jsonPath("$.next.title").value("글 7"));
        mvc.perform(get("/api/blogs/page-blog/posts/" + ids.get(4)).session(owner))
                .andExpect(jsonPath("$.next.title").value("글 6"));
        mvc.perform(get("/api/blogs/page-blog/posts/" + ids.get(0))).andExpect(jsonPath("$.prev").doesNotExist());

        // 검색: 대소문자 무시, 앞뒤 공백 무시, %·_는 글자 그대로, 태그로도
        mvc.perform(get("/api/blogs/page-blog/posts").param("q", "  spring_boot ")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/blogs/page-blog/posts").param("q", "100%")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/blogs/page-blog/posts").param("q", "_")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/blogs/page-blog/posts").param("q", "JAVA")).andExpect(jsonPath("$.items[0].title").value("글 5"));
        mvc.perform(get("/api/blogs/page-blog/posts").param("q", "   ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/blogs/page-blog/posts").param("q", "없는말")).andExpect(jsonPath("$.items", hasSize(0)));
        // 태그별 목록, 태그 목록
        mvc.perform(get("/api/blogs/page-blog/posts").param("tag", "java")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/blogs/page-blog/tags")).andExpect(jsonPath("$[0].name").value("Java")).andExpect(jsonPath("$[0].count").value(1));
    }

    @Test
    void home_cursor_has_no_duplicates_home01() throws Exception {
        MockHttpSession a = blogOwner("home-a", "home-a");
        MockHttpSession b = blogOwner("home-b", "home-b");
        for (int i = 0; i < 12; i++) {
            publish(i % 2 == 0 ? a : b, "홈 글 " + i);
        }
        Set<Long> seen = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do {
            var req = get("/api/home/posts");
            if (cursor != null) {
                req.param("cursor", cursor);
            }
            JsonNode page = read(mvc.perform(req).andExpect(status().isOk()));
            for (JsonNode item : page.get("items")) {
                assertThat(seen.add(item.get("id").asLong())).as("중복 없음").isTrue();
                assertThat(item.get("blog").get("name").asText()).isNotBlank();
            }
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asText();
            pages++;
        } while (cursor != null && pages < 20);
        assertThat(seen.size()).isGreaterThanOrEqualTo(12);
    }

    @Test
    void comments_cmt01_cmt02_cmt03() throws Exception {
        MockHttpSession owner = blogOwner("cmt-owner", "cmt-blog");
        long postId = publish(owner, "댓글 받을 글");
        String url = "/api/posts/" + postId + "/comments";
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"익명\"}"))
                .andExpect(status().isUnauthorized());
        MockHttpSession a = login("cmt-a");
        MockHttpSession b = login("cmt-b");
        long ca = read(send(post(url), a, Map.of("content", "첫 댓글")).andExpect(status().isCreated())).get("id").asLong();
        long cb = read(send(post(url), b, Map.of("content", "두 번째")).andExpect(status().isCreated())).get("id").asLong();
        send(post(url), a, Map.of("content", "가".repeat(1001))).andExpect(status().isBadRequest());
        mvc.perform(get(url).session(a))
                .andExpect(jsonPath("$[0].content").value("첫 댓글"))
                .andExpect(jsonPath("$[0].nickname").value("cmt-a"))
                .andExpect(jsonPath("$[0].canEdit").value(true))
                .andExpect(jsonPath("$[1].canDelete").value(false));
        mvc.perform(get("/api/blogs/cmt-blog/posts/" + postId)).andExpect(jsonPath("$.commentCount").value(2));

        // 수정은 본인만 (글 주인도 못 고침)
        send(patch("/api/comments/" + ca), b, Map.of("content", "바꿈")).andExpect(status().isForbidden());
        send(patch("/api/comments/" + ca), owner, Map.of("content", "바꿈")).andExpect(status().isForbidden());
        send(patch("/api/comments/" + ca), a, Map.of("content", "고친 댓글")).andExpect(jsonPath("$.content").value("고친 댓글"));
        // 삭제: 남은 403, 글 주인은 됨, 본인도 됨
        mvc.perform(delete("/api/comments/" + ca).session(b)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/comments/" + cb).session(owner)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/comments/" + ca).session(a)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void likes_soc01_and_delete_cascade_post03() throws Exception {
        MockHttpSession owner = blogOwner("like-owner", "like-blog");
        long postId = publish(owner, "공감 받을 글");
        String url = "/api/posts/" + postId + "/like";
        mvc.perform(post(url)).andExpect(status().isUnauthorized());
        mvc.perform(post(url).session(owner)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OWN_POST"));
        MockHttpSession a = login("like-a");
        mvc.perform(post(url).session(a)).andExpect(jsonPath("$.likeCount").value(1)).andExpect(jsonPath("$.liked").value(true));
        mvc.perform(post(url).session(a)).andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(post(url).session(login("like-b"))).andExpect(jsonPath("$.likeCount").value(2));
        mvc.perform(get("/api/blogs/like-blog/posts/" + postId).session(a))
                .andExpect(jsonPath("$.likeCount").value(2)).andExpect(jsonPath("$.liked").value(true));
        mvc.perform(delete(url).session(a)).andExpect(jsonPath("$.likeCount").value(1)).andExpect(jsonPath("$.liked").value(false));

        send(post("/api/posts/" + postId + "/comments"), a, Map.of("content", "지워질 댓글"));
        Map<String, Object> tagged = postBody("공감 받을 글", "<p>x</p>");
        tagged.put("tags", List.of("지울태그"));
        send(put("/api/posts/" + postId), owner, tagged);

        mvc.perform(delete("/api/posts/" + postId).session(a)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/" + postId).session(owner)).andExpect(status().isNoContent());
        mvc.perform(get("/api/blogs/like-blog/posts/" + postId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/blogs/like-blog/tags")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void tags_limit_and_html_sanitize() throws Exception {
        MockHttpSession owner = blogOwner("safe-owner", "safe-blog");
        Map<String, Object> many = postBody("태그 많음", "<p>x</p>");
        List<String> eleven = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            eleven.add("t" + i);
        }
        many.put("tags", eleven);
        send(post("/api/posts"), owner, many).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields[0].field").value("tags"));

        long id = publish(owner, postBody("스크립트",
                "<p onclick=\"alert(1)\">안전</p><script>alert(1)</script><img src=\"x\" onerror=\"alert(2)\"><a href=\"javascript:alert(3)\">링크</a>"));
        String html = read(mvc.perform(get("/api/blogs/safe-blog/posts/" + id))).get("contentHtml").asText();
        assertThat(html).contains("안전").doesNotContain("<script", "onclick", "onerror", "javascript:");
    }

    @Test
    void images_post05_post07_auth05_blog02() throws Exception {
        MockHttpSession owner = blogOwner("img-owner", "img-blog");
        mvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "a.png", "image/png", png())))
                .andExpect(status().isUnauthorized());
        // 확장자가 png여도 내용이 이미지가 아니면 거절
        mvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "fake.png", "image/png", "hello".getBytes()))
                .session(owner)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields[0].field").value("file"));
        JsonNode img = read(mvc.perform(multipart("/api/images")
                .file(new MockMultipartFile("file", "photo.bin", "application/octet-stream", png())).session(owner))
                .andExpect(status().isCreated()));
        String url = img.get("url").asText();
        assertThat(url).startsWith("/files/").endsWith(".png");
        mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(get(img.get("thumbUrl").asText())).andExpect(status().isOk());

        // 대표 이미지: 안 고르면 본문 첫 이미지의 썸네일
        long id = publish(owner, postBody("사진 글", "<p>사진</p><img src=\"" + url + "\">"));
        mvc.perform(get("/api/blogs/img-blog/posts")).andExpect(jsonPath("$.items[0].thumbnailUrl").value(img.get("thumbUrl").asText()));
        // 이미지만 있어도 본문으로 인정
        publish(owner, postBody("사진만", "<img src=\"" + url + "\">"));
        // 남이 올린 이미지는 대표로 못 고른다
        Map<String, Object> body = postBody("남의 사진", "<p>x</p>");
        body.put("thumbnailImageId", img.get("id").asLong());
        send(put("/api/posts/" + id), owner, body).andExpect(status().isOk());
        MockHttpSession other = blogOwner("img-other", "img-other");
        send(post("/api/posts"), other, body).andExpect(status().isBadRequest());

        // 회원정보·블로그 프로필 이미지
        send(patch("/api/me"), owner, Map.of("nickname", "새닉네임", "profileImageUrl", url))
                .andExpect(jsonPath("$.nickname").value("새닉네임")).andExpect(jsonPath("$.profileImageUrl").value(url));
        send(patch("/api/me"), owner, Map.of("nickname", "x", "profileImageUrl", "https://evil.example/a.png"))
                .andExpect(status().isBadRequest());
        send(patch("/api/blogs/img-blog"), owner, Map.of("name", "사진 블로그", "profileImageUrl", url))
                .andExpect(jsonPath("$.profileImageUrl").value(url));
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
