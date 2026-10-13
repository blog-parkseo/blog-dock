package com.blogdock.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.blogdock.auth.Member;
import com.blogdock.auth.MemberRepository;
import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogAccess;
import com.blogdock.blog.BlogRepository;
import com.blogdock.common.ApiException;
import com.blogdock.notification.NotificationEvents.BlogSubscribed;
import com.blogdock.notification.NotificationEvents.CommentWritten;
import com.blogdock.notification.NotificationEvents.PostLiked;
import com.blogdock.notification.NotificationEvents.PostPublished;
import com.blogdock.post.Post;
import com.blogdock.post.PostRepository;
import com.blogdock.post.PostService;
import com.blogdock.subscription.Subscription;
import com.blogdock.subscription.SubscriptionRepository;

/** NOTI-01 알림 만들기, NOTI-02 알림 목록·읽음, NOTI-03 오래된 알림 지우기. */
@Service
public class NotificationService {

    public static final int PAGE_SIZE = 20;
    static final Duration LIKE_ONCE_PER = Duration.ofDays(1);
    static final Duration KEEP = Duration.ofDays(90);
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final PostRepository postRepository;
    private final BlogRepository blogRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BlogAccess blogAccess;

    public NotificationService(NotificationRepository notificationRepository, PostRepository postRepository,
                               BlogRepository blogRepository, MemberRepository memberRepository,
                               SubscriptionRepository subscriptionRepository, BlogAccess blogAccess) {
        this.notificationRepository = notificationRepository;
        this.postRepository = postRepository;
        this.blogRepository = blogRepository;
        this.memberRepository = memberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.blogAccess = blogAccess;
    }

    // ---------- 만들기 (원래 일이 커밋된 뒤, 따로 된 트랜잭션에서) ----------

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(CommentWritten e) {
        safely(() -> toPostOwner(e.postId(), e.actorId(), Notification.COMMENT));
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PostLiked e) {
        safely(() -> toPostOwner(e.postId(), e.actorId(), Notification.LIKE));
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(BlogSubscribed e) {
        safely(() -> blogRepository.findById(e.blogId()).ifPresent(blog -> {
            if (!blog.isOwnedBy(e.actorId())) {
                notificationRepository.save(Notification.of(blog.getOwnerId(), e.actorId(), Notification.SUBSCRIBE,
                        blog.getId(), null, nickname(e.actorId()), blog.getName(), null));
            }
        }));
    }

    /** 새 글은 그 블로그의 구독자 모두에게. 구독자 공개 글도 구독자에게만 가므로 보낸다. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PostPublished e) {
        safely(() -> postRepository.findById(e.postId()).ifPresent(post -> {
            if (!post.isPublished() || !(post.isPublic() || post.isSubscribersOnly())) {
                return;
            }
            Blog blog = blogAccess.find(post.getBlogId());
            List<Notification> items = subscriptionRepository.findUserIdsByBlogId(blog.getId()).stream()
                    .filter(userId -> !blog.isOwnedBy(userId))
                    .map(userId -> Notification.of(userId, blog.getOwnerId(), Notification.NEW_POST, blog.getId(),
                            post.getId(), null, blog.getName(), post.getTitle()))
                    .toList();
            notificationRepository.saveAll(items);
        }));
    }

    private void toPostOwner(Long postId, Long actorId, String type) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return;
        }
        Blog blog = blogAccess.find(post.getBlogId());
        Long owner = blog.getOwnerId();
        if (owner.equals(actorId)) {
            return; // 내 행동은 나에게 알리지 않는다
        }
        if (Notification.LIKE.equals(type) && notificationRepository
                .existsByRecipientIdAndActorIdAndPostIdAndTypeAndCreatedAtAfter(owner, actorId, postId, type,
                        Instant.now().minus(LIKE_ONCE_PER))) {
            return;
        }
        notificationRepository.save(Notification.of(owner, actorId, type, blog.getId(), postId,
                nickname(actorId), blog.getName(), post.getTitle()));
    }

    private void safely(Runnable work) {
        try {
            work.run();
        } catch (RuntimeException ex) {
            log.warn("알림을 만들지 못했어요", ex);
        }
    }

    private String nickname(Long memberId) {
        return memberRepository.findById(memberId).map(Member::getNickname).orElse("알 수 없음");
    }

    // ---------- 읽기 ----------

    @Transactional(readOnly = true)
    public long unreadCount(Long memberId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(memberId);
    }

    @Transactional(readOnly = true)
    public NotificationPage list(Long memberId, String cursor) {
        Long after = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                after = Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                throw ApiException.invalid("cursor", "잘못된 위치예요");
            }
        }
        List<Notification> rows = notificationRepository.findPage(memberId, after, PageRequest.of(0, PAGE_SIZE + 1));
        boolean more = rows.size() > PAGE_SIZE;
        List<Notification> items = more ? rows.subList(0, PAGE_SIZE) : rows;
        String next = more ? String.valueOf(items.get(items.size() - 1).getId()) : null;
        return new NotificationPage(items(memberId, items), next);
    }

    /** 누르면 읽음으로 바꾸고, 어디로 갈지(또는 볼 수 없는지) 알려 준다. */
    @Transactional
    public NotificationItem read(Long id, Long memberId) {
        Notification n = notificationRepository.findById(id)
                .filter(x -> x.getRecipientId().equals(memberId))
                .orElseThrow(() -> ApiException.notFound("알림을 찾을 수 없어요"));
        n.read();
        return items(memberId, List.of(n)).get(0);
    }

    @Transactional
    public long readAll(Long memberId) {
        return notificationRepository.readAll(memberId, Instant.now());
    }

    /** 매일 새벽 4시(한국 시간)에 90일이 지난 알림을 지운다. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        int n = notificationRepository.deleteOlderThan(Instant.now().minus(KEEP));
        if (n > 0) {
            log.info("90일이 지난 알림 {}개를 지웠어요", n);
        }
    }

    /** 지금도 볼 수 있는 글인지 알림을 받는 사람 기준으로 다시 확인한다. */
    private List<NotificationItem> items(Long memberId, List<Notification> list) {
        Map<Long, Post> posts = byId(postRepository.findAllById(list.stream().map(Notification::getPostId)
                .filter(Objects::nonNull).collect(Collectors.toSet())), Post::getId);
        Map<Long, Blog> blogs = byId(blogRepository.findAllById(list.stream().map(Notification::getBlogId)
                .filter(Objects::nonNull).collect(Collectors.toSet())), Blog::getId);
        Map<Long, Boolean> subscriberOf = new HashMap<>();
        return list.stream().map(n -> {
            String address = null;
            String slug = null;
            boolean available = true;
            boolean subscribed = false;
            if (n.getPostId() != null) {
                Post post = posts.get(n.getPostId());
                Blog blog = post == null ? null : blogs.get(post.getBlogId());
                available = blog != null && PostService.canView(post, blog.isOwnedBy(memberId),
                        subscriberOf.computeIfAbsent(blog.getId(), k -> blogAccess.isSubscriber(blog, memberId)));
                if (available) {
                    address = blog.getAddress();
                    slug = post.getSlug();
                }
            } else if (Notification.SUBSCRIBE.equals(n.getType()) && n.getActorId() != null) {
                // 구독한 사람의 블로그가 있으면 그리로 가고, 알림 화면에서 바로 맞구독할 수 있다
                Blog actorBlog = blogRepository.findByOwnerId(n.getActorId()).orElse(null);
                if (actorBlog != null) {
                    address = actorBlog.getAddress();
                    subscribed = subscriptionRepository.existsById(new Subscription.Key(memberId, actorBlog.getId()));
                }
            }
            return new NotificationItem(n.getId(), n.getType(), message(n), address, slug, available,
                    n.getReadAt() != null, n.getCreatedAt(), subscribed);
        }).toList();
    }

    static String message(Notification n) {
        return switch (n.getType()) {
            case Notification.COMMENT -> n.getActorName() + "님이 “" + n.getPostTitle() + "”에 댓글을 남겼어요";
            case Notification.LIKE -> n.getActorName() + "님이 “" + n.getPostTitle() + "”에 공감했어요";
            case Notification.SUBSCRIBE -> n.getActorName() + "님이 내 블로그를 구독했어요";
            default -> n.getBlogName() + "에 새 글 “" + n.getPostTitle() + "”이 올라왔어요";
        };
    }

    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> id) {
        Map<Long, T> map = new HashMap<>();
        items.forEach(i -> map.put(id.apply(i), i));
        return map;
    }

    /**
     * address·slug: 누르면 갈 곳 (slug가 없으면 블로그, 둘 다 없으면 이동 없음). available=false면 볼 수 없는 글.
     * subscribed: 구독 알림에서 내가 그 사람 블로그를 이미 구독했는지 (맞구독 버튼 상태). 블로그가 없으면 address가 null.
     */
    public record NotificationItem(Long id, String type, String message, String address, String slug,
                                   boolean available, boolean read, Instant createdAt, boolean subscribed) {
    }

    public record NotificationPage(List<NotificationItem> items, String nextCursor) {
    }
}
