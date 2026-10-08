package com.blogdock.blog;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.blog.BlogDtos.AddressCheckResponse;
import com.blogdock.blog.BlogDtos.BlogResponse;
import com.blogdock.blog.BlogDtos.CreateRequest;
import com.blogdock.blog.BlogDtos.UpdateRequest;

import jakarta.validation.Valid;

@RestController
public class BlogController {

    private final BlogService blogService;
    private final SidebarService sidebarService;

    public BlogController(BlogService blogService, SidebarService sidebarService) {
        this.blogService = blogService;
        this.sidebarService = sidebarService;
    }

    @GetMapping("/api/blogs/{address}/sidebar")
    public SidebarService.Sidebar sidebar(@PathVariable String address, @AuthenticationPrincipal LoginMember login) {
        return sidebarService.get(address, LoginMember.idOf(login));
    }

    @GetMapping("/api/blogs/address-check")
    public AddressCheckResponse checkAddress(@RequestParam String address) {
        return blogService.checkAddress(address);
    }

    @PostMapping("/api/blogs")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogResponse create(@AuthenticationPrincipal LoginMember login, @Valid @RequestBody CreateRequest req) {
        return blogService.create(LoginMember.require(login), req);
    }

    /** 비회원도 볼 수 있다. 로그인했으면 주인인지(isOwner) 알려 준다. */
    @GetMapping("/api/blogs/{address}")
    public BlogResponse get(@PathVariable String address, @AuthenticationPrincipal LoginMember login) {
        return blogService.get(address, LoginMember.idOf(login));
    }

    @PatchMapping("/api/blogs/{address}")
    public BlogResponse update(@PathVariable String address, @AuthenticationPrincipal LoginMember login,
                               @Valid @RequestBody UpdateRequest req) {
        return blogService.update(address, LoginMember.require(login), req);
    }
}
