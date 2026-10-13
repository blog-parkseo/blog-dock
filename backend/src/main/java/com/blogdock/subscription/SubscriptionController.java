package com.blogdock.subscription;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.post.PostDtos.HomeResponse;
import com.blogdock.post.PostService;
import com.blogdock.subscription.SubscriptionService.SubscribedBlog;
import com.blogdock.subscription.SubscriptionService.SubscriptionResponse;

/** SUB-01 구독·해제, SUB-02 구독 목록, SUB-03 구독 피드. 모두 로그인이 필요하다. */
@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PostService postService;

    public SubscriptionController(SubscriptionService subscriptionService, PostService postService) {
        this.subscriptionService = subscriptionService;
        this.postService = postService;
    }

    @PostMapping("/api/blogs/{address}/subscription")
    public SubscriptionResponse subscribe(@PathVariable String address, @AuthenticationPrincipal LoginMember login) {
        return subscriptionService.subscribe(address, LoginMember.require(login));
    }

    @DeleteMapping("/api/blogs/{address}/subscription")
    public SubscriptionResponse unsubscribe(@PathVariable String address, @AuthenticationPrincipal LoginMember login) {
        return subscriptionService.unsubscribe(address, LoginMember.require(login));
    }

    @GetMapping("/api/me/subscriptions")
    public List<SubscribedBlog> mine(@AuthenticationPrincipal LoginMember login) {
        return subscriptionService.mine(LoginMember.require(login));
    }

    /** 구독한 블로그의 공개 글을 최신순으로. 홈과 같은 커서 방식. */
    @GetMapping("/api/feed")
    public HomeResponse feed(@RequestParam(required = false) String cursor, @AuthenticationPrincipal LoginMember login) {
        return postService.feed(LoginMember.require(login), cursor);
    }
}
