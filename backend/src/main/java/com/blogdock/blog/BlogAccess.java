package com.blogdock.blog;

import org.springframework.stereotype.Component;

import com.blogdock.common.ApiException;

/** 블로그 찾기와 주인 확인을 여러 기능이 같이 쓴다 (COM-01). */
@Component
public class BlogAccess {

    private final BlogRepository blogRepository;

    public BlogAccess(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    /** 없는 블로그는 404. */
    public Blog find(String address) {
        return blogRepository.findByAddress(address)
                .orElseThrow(() -> ApiException.notFound("블로그를 찾을 수 없어요"));
    }

    public Blog find(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> ApiException.notFound("블로그를 찾을 수 없어요"));
    }

    /** 주인이 아니면 403. */
    public Blog findOwned(String address, Long memberId) {
        Blog blog = find(address);
        checkOwner(blog, memberId);
        return blog;
    }

    public Blog checkOwner(Long blogId, Long memberId) {
        Blog blog = find(blogId);
        checkOwner(blog, memberId);
        return blog;
    }

    private void checkOwner(Blog blog, Long memberId) {
        if (!blog.isOwnedBy(memberId)) {
            throw ApiException.forbidden("내 블로그에서만 할 수 있어요");
        }
    }
}
