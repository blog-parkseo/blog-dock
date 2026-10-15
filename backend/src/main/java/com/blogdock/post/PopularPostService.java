package com.blogdock.post;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.post.PostDtos.PostSummary;

/**
 * HOME-02 인기 글. 최근 7일 동안 받은 공감 수 + 댓글 수(글쓴이 본인 댓글 제외)가 큰 순서로 10개.
 * 동점이면 최근에 발행한 글 먼저, 점수가 0인 글은 넣지 않는다.
 * 10분마다 다시 계산해 메모리에 들고 있고, 보여 주기 직전에 아직 공개 글인지 한 번 더 확인한다.
 */
@Service
public class PopularPostService {

    static final int SIZE = 10;
    private static final Duration WINDOW = Duration.ofDays(7);

    private final PostRepository postRepository;
    private final PostService postService;
    private volatile List<Long> ranking = List.of();

    public PopularPostService(PostRepository postRepository, PostService postService) {
        this.postRepository = postRepository;
        this.postService = postService;
    }

    /** 서버가 뜰 때 한 번, 그 뒤 10분마다. */
    @Scheduled(initialDelay = 0, fixedRate = 10 * 60 * 1000)
    @Transactional(readOnly = true)
    public void refresh() {
        Instant since = Instant.now().minus(WINDOW);
        Map<Long, Long> score = new HashMap<>();
        for (Object[] row : postRepository.countRecentLikes(since)) {
            score.merge((Long) row[0], (Long) row[1], Long::sum);
        }
        for (Object[] row : postRepository.countRecentComments(since)) {
            score.merge((Long) row[0], (Long) row[1], Long::sum);
        }
        ranking = postRepository.findAllById(score.keySet()).stream()
                .sorted(Comparator.comparing((Post p) -> score.get(p.getId())).reversed()
                        .thenComparing(Post::getPublishedAt, Comparator.reverseOrder())
                        .thenComparing(Post::getId, Comparator.reverseOrder()))
                .limit(SIZE)
                .map(Post::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostSummary> popular() {
        List<Long> ids = ranking;
        if (ids.isEmpty()) {
            return List.of();
        }
        // 계산한 뒤 비공개로 바꾸거나 지운 글은 빼고, 순서는 그대로 둔다
        Map<Long, Post> byId = postRepository.findAllById(ids).stream()
                .filter(p -> p.isPublished() && p.isPublic())
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        return postService.summaries(ids.stream().filter(byId::containsKey).map(byId::get).toList());
    }
}
