package com.blogdock.subscription;

import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogAccess;
import com.blogdock.blog.BlogRepository;
import com.blogdock.common.ApiException;
import com.blogdock.notification.NotificationEvents.BlogSubscribed;
import com.blogdock.notification.NotificationEvents.ReservationFulfilled;

/** SUB-01 구독·해제. 누르면 구독, 다시 누르면 해제. 구독자 수는 저장하지 않고 매번 센다. */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final BlogAccess blogAccess;
    private final BlogRepository blogRepository;
    private final SubscriptionReservationRepository reservationRepository;
    private final ApplicationEventPublisher events;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, BlogAccess blogAccess,
                               BlogRepository blogRepository, SubscriptionReservationRepository reservationRepository,
                               ApplicationEventPublisher events) {
        this.subscriptionRepository = subscriptionRepository;
        this.blogAccess = blogAccess;
        this.blogRepository = blogRepository;
        this.reservationRepository = reservationRepository;
        this.events = events;
    }

    /**
     * SUB-07 예약 맞구독. 상대가 이미 블로그가 있으면 바로 구독한다.
     * 돌려주는 값: true면 예약됨, false면 바로 구독됨.
     */
    @Transactional
    public boolean reserve(Long memberId, Long targetMemberId) {
        if (memberId.equals(targetMemberId)) {
            throw ApiException.badRequest("OWN_BLOG", "내 블로그는 구독할 수 없어요");
        }
        Blog blog = blogRepository.findByOwnerId(targetMemberId).orElse(null);
        if (blog != null) {
            subscribe(blog.getAddress(), memberId);
            return false;
        }
        if (!reservationRepository.existsById(new SubscriptionReservation.Key(memberId, targetMemberId))) {
            reservationRepository.save(new SubscriptionReservation(memberId, targetMemberId));
        }
        return true;
    }

    @Transactional
    public void cancelReservation(Long memberId, Long targetMemberId) {
        reservationRepository.deleteById(new SubscriptionReservation.Key(memberId, targetMemberId));
    }

    @Transactional(readOnly = true)
    public boolean isReserved(Long memberId, Long targetMemberId) {
        return reservationRepository.existsById(new SubscriptionReservation.Key(memberId, targetMemberId));
    }

    /** 블로그를 새로 만들면 이 사람에게 걸린 예약을 모두 구독으로 바꾼다 (블로그 만들기와 같은 트랜잭션). */
    @Transactional
    public void fulfillReservations(Blog blog) {
        List<SubscriptionReservation> reservations = reservationRepository.findByTargetMemberId(blog.getOwnerId());
        for (SubscriptionReservation r : reservations) {
            Long userId = r.getUserId();
            if (!subscriptionRepository.existsById(new Subscription.Key(userId, blog.getId()))) {
                subscriptionRepository.save(new Subscription(userId, blog.getId()));
                events.publishEvent(new BlogSubscribed(blog.getId(), userId));
                events.publishEvent(new ReservationFulfilled(blog.getId(), userId));
            }
        }
        reservationRepository.deleteAll(reservations);
    }

    @Transactional
    public SubscriptionResponse subscribe(String address, Long memberId) {
        Blog blog = blogAccess.find(address);
        if (blog.isOwnedBy(memberId)) {
            throw ApiException.badRequest("OWN_BLOG", "내 블로그는 구독할 수 없어요");
        }
        Subscription.Key key = new Subscription.Key(memberId, blog.getId());
        if (!subscriptionRepository.existsById(key)) {
            try {
                subscriptionRepository.saveAndFlush(new Subscription(memberId, blog.getId()));
                events.publishEvent(new BlogSubscribed(blog.getId(), memberId));
            } catch (DataIntegrityViolationException e) {
                // 동시에 두 번 눌렀다: 이미 하나 있으니 괜찮다
            }
        }
        return status(blog, memberId);
    }

    @Transactional
    public SubscriptionResponse unsubscribe(String address, Long memberId) {
        Blog blog = blogAccess.find(address);
        // 내 구독만 지운다. 상대가 나를 구독한 것은 그대로다 (SUB-05)
        subscriptionRepository.deleteById(new Subscription.Key(memberId, blog.getId()));
        return status(blog, memberId);
    }

    @Transactional(readOnly = true)
    public List<SubscribedBlog> mine(Long memberId) {
        // 내 블로그를 구독한 사람들. 내가 구독한 블로그의 주인이 여기 있으면 맞구독이다
        Set<Long> subscribesMe = blogRepository.findByOwnerId(memberId)
                .map(mine -> Set.copyOf(subscriptionRepository.findUserIdsByBlogId(mine.getId())))
                .orElse(Set.of());
        return subscriptionRepository.findSubscribedBlogs(memberId).stream()
                .map(b -> new SubscribedBlog(b.getAddress(), b.getName(), b.getDescription(), b.getProfileImageUrl(),
                        subscribesMe.contains(b.getOwnerId())))
                .toList();
    }

    /**
     * 블로그 화면에 보여 줄 구독자 수, 내가 구독했는지, 블로그 주인이 내 블로그를 구독했는지(SUB-05).
     * 두 가지가 모두 참이면 맞구독이다. 내 블로그가 없으면 subscribesMe는 항상 거짓이다.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse status(Blog blog, Long viewerId) {
        boolean subscribed = false;
        boolean subscribesMe = false;
        if (viewerId != null && !blog.isOwnedBy(viewerId)) {
            subscribed = subscriptionRepository.existsById(new Subscription.Key(viewerId, blog.getId()));
            subscribesMe = blogRepository.findByOwnerId(viewerId)
                    .map(mine -> subscriptionRepository.existsById(new Subscription.Key(blog.getOwnerId(), mine.getId())))
                    .orElse(false);
        }
        return new SubscriptionResponse(subscribed, subscribesMe, subscriptionRepository.countByBlogId(blog.getId()));
    }

    public record SubscriptionResponse(boolean subscribed, boolean subscribesMe, long subscriberCount) {
    }

    /** mutual: 서로 구독(맞구독) 중인지. */
    public record SubscribedBlog(String address, String name, String description, String profileImageUrl,
                                 boolean mutual) {
    }
}
