package com.blogdock.post;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.common.Idempotency;
import com.blogdock.post.PostDtos.HomeResponse;
import com.blogdock.post.PostDtos.PageResponse;
import com.blogdock.post.PostDtos.PostDetail;
import com.blogdock.post.PostDtos.PostEdit;
import com.blogdock.post.PostDtos.PostSummary;
import com.blogdock.post.PostDtos.SavedResponse;
import com.blogdock.post.PostDtos.VisibilityRequest;
import com.blogdock.post.PostDtos.WriteRequest;

import jakarta.validation.Valid;

@RestController
public class PostController {

    private final PostService postService;
    private final Idempotency idempotency;

    public PostController(PostService postService, Idempotency idempotency) {
        this.postService = postService;
        this.idempotency = idempotency;
    }

    /** 블로그 글 목록. category=id|none, tag=이름, q=검색어 (모두 선택). */
    @GetMapping("/api/blogs/{address}/posts")
    public PageResponse<PostSummary> list(@PathVariable String address,
                                          @AuthenticationPrincipal LoginMember login,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String tag,
                                          @RequestParam(required = false) String q) {
        return postService.list(address, LoginMember.idOf(login), page, category, tag, q);
    }

    @GetMapping("/api/blogs/{address}/posts/{slug}")
    public PostDetail detail(@PathVariable String address, @PathVariable String slug,
                             @AuthenticationPrincipal LoginMember login) {
        return postService.detail(address, slug, LoginMember.idOf(login));
    }

    @GetMapping("/api/home/posts")
    public HomeResponse home(@RequestParam(required = false) String cursor) {
        return postService.home(cursor);
    }

    /** 새 글. 같은 Idempotency-Key로 다시 보내면 처음 결과를 돌려준다. */
    @PostMapping("/api/posts")
    public ResponseEntity<?> create(@AuthenticationPrincipal LoginMember login,
                                    @RequestHeader(value = Idempotency.HEADER, required = false) String key,
                                    @Valid @RequestBody WriteRequest req) {
        Long memberId = LoginMember.require(login);
        return idempotency.run(memberId, key, req, HttpStatus.CREATED, () -> postService.create(memberId, req));
    }

    @GetMapping("/api/posts/{id}/edit")
    public PostEdit edit(@PathVariable Long id, @AuthenticationPrincipal LoginMember login) {
        return postService.edit(id, LoginMember.require(login));
    }

    @PutMapping("/api/posts/{id}")
    public SavedResponse update(@PathVariable Long id, @AuthenticationPrincipal LoginMember login,
                                @Valid @RequestBody WriteRequest req) {
        return postService.update(id, LoginMember.require(login), req);
    }

    @PatchMapping("/api/posts/{id}/visibility")
    public SavedResponse visibility(@PathVariable Long id, @AuthenticationPrincipal LoginMember login,
                                    @RequestBody VisibilityRequest req) {
        return postService.changeVisibility(id, LoginMember.require(login), req.visibility());
    }

    @DeleteMapping("/api/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal LoginMember login) {
        postService.delete(id, LoginMember.require(login));
    }

    /** MNG-01 내 글 관리. status=PUBLIC|PRIVATE|DRAFT */
    @GetMapping("/api/me/posts")
    public PageResponse<PostSummary> mine(@AuthenticationPrincipal LoginMember login,
                                          @RequestParam(defaultValue = "PUBLIC") String status,
                                          @RequestParam(defaultValue = "0") int page) {
        return postService.mine(LoginMember.require(login), status, page);
    }
}
