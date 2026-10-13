package com.blogdock.subscription;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogDtos.BlogResponse;
import com.blogdock.blog.BlogRepository;
import com.blogdock.post.PostRepository;

/**
 * HOME-04 인기 블로거. 최근 7일 동안 블로그의 공개 글이 받은 공감 수 + 댓글 수(블로그 주인 본인 댓글 제외)
 * + 최근 7일 새 구독자 수가 큰 순서로 10개. 공개 글이 없거나 점수가 0인 블로그는 뺀다.
 * 동점이면 전체 구독자가 많은 블로그, 그다음 최근에 만든 블로그가 먼저다.
 * 순위는 10분마다 모두에게 같게 계산하고, 구독 버튼 상태는 보는 사람마다 그때그때 확인한다.
 */
@Service
public class PopularBloggerService {

    static final int SIZE = 10;
    private static final Duration WINDOW = Duration.ofDays(7);

    private final PostRepository postRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BlogRepository blogRepository;
    private final SubscriptionService subscriptionService;
    private volatile List<Long> ranking = List.of();

    public PopularBloggerService(PostRepository postRepository, SubscriptionRepository subscriptionRepository,
                                 BlogRepository blogRepository, SubscriptionService subscriptionService) {
        this.postRepository = postRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.blogRepository = blogRepository;
        this.subscriptionService = subscriptionService;
    }

    /** 서버가 뜰 때 한 번, 그 뒤 10분마다. */
    @Scheduled(initialDelay = 0, fixedRate = 10 * 60 * 1000)
    @Transactional(readOnly = true)
    public void refresh() {
        Instant since = Instant.now().minus(WINDOW);
        Map<Long, Long> score = new HashMap<>();
        for (List<Object[]> rows : List.of(postRepository.countRecentLikesByBlog(since),
                postRepository.countRecentCommentsByBlog(since), subscriptionRepository.countRecentByBlog(since))) {
            for (Object[] row : rows) {
                score.merge((Long) row[0], (Long) row[1], Long::sum);
            }
        }
        if (score.isEmpty()) {
            ranking = List.of();
            return;
        }
        Set<Long> withPosts = new HashSet<>(postRepository.findBlogIdsWithPublicPosts(score.keySet()));
        Map<Long, Long> total = new HashMap<>();
        for (Object[] row : subscriptionRepository.countByBlogIds(withPosts)) {
            total.put((Long) row[0], (Long) row[1]);
        }
        ranking = withPosts.stream()
                .sorted(Comparator.comparing((Long id) -> score.get(id)).reversed()
                        .thenComparing((Long id) -> total.getOrDefault(id, 0L), Comparator.reverseOrder())
                        .thenComparing(Comparator.reverseOrder()))
                .limit(SIZE)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BlogResponse> popular(Long viewerId) {
        List<Long> ids = ranking;
        if (ids.isEmpty()) {
            return List.of();
        }
        // 계산한 뒤 지워진 블로그는 빼고, 순서는 그대로 둔다
        Map<Long, Blog> byId = blogRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Blog::getId, Function.identity()));
        return ids.stream().filter(byId::containsKey).map(byId::get)
                .map(b -> BlogResponse.of(b, viewerId, subscriptionService.status(b, viewerId)))
                .toList();
    }
}
