package com.blogdock.blog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.category.CategoryRepository;
import com.blogdock.post.PostRepository;
import com.blogdock.subscription.SubscriptionService;

/**
 * BLOG-04 사이드바: 블로그 이름·프로필, 카테고리 목록과 글 수.
 * '전체 글'은 맨 위, '미분류'는 맨 아래, 미분류 글이 없으면 숨긴다(uncategorizedCount=0).
 * 비공개 글은 주인이 아니면 글 수에서 빠진다.
 */
@Service
public class SidebarService {

    private final BlogAccess blogAccess;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final SubscriptionService subscriptionService;

    public SidebarService(BlogAccess blogAccess, CategoryRepository categoryRepository, PostRepository postRepository,
                          SubscriptionService subscriptionService) {
        this.blogAccess = blogAccess;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(readOnly = true)
    public Sidebar get(String address, Long viewerId) {
        Blog blog = blogAccess.find(address);
        boolean owner = blog.isOwnedBy(viewerId);
        Map<Long, Long> counts = new HashMap<>();
        long total = 0;
        for (Object[] row : postRepository.countByCategory(blog.getId(), owner)) {
            long n = (Long) row[1];
            counts.put((Long) row[0], n);
            total += n;
        }
        List<CategoryCount> categories = categoryRepository.findByBlogIdOrderBySortOrderAscIdAsc(blog.getId()).stream()
                .map(c -> new CategoryCount(c.getId(), c.getName(), counts.getOrDefault(c.getId(), 0L)))
                .toList();
        BlogDtos.BlogResponse info = BlogDtos.BlogResponse.of(blog, viewerId,
                subscriptionService.status(blog, viewerId));
        return new Sidebar(info, total, categories, counts.getOrDefault(null, 0L));
    }

    public record CategoryCount(Long id, String name, long count) {
    }

    public record Sidebar(BlogDtos.BlogResponse blog, long totalCount, List<CategoryCount> categories,
                          long uncategorizedCount) {
    }
}
