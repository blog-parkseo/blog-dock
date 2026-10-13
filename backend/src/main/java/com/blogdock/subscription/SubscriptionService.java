package com.blogdock.subscription;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogAccess;
import com.blogdock.common.ApiException;

/** SUB-01 구독·해제. 누르면 구독, 다시 누르면 해제. 구독자 수는 저장하지 않고 매번 센다. */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final BlogAccess blogAccess;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, BlogAccess blogAccess) {
        this.subscriptionRepository = subscriptionRepository;
        this.blogAccess = blogAccess;
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
            } catch (DataIntegrityViolationException e) {
                // 동시에 두 번 눌렀다: 이미 하나 있으니 괜찮다
            }
        }
        return new SubscriptionResponse(true, subscriptionRepository.countByBlogId(blog.getId()));
    }

    @Transactional
    public SubscriptionResponse unsubscribe(String address, Long memberId) {
        Blog blog = blogAccess.find(address);
        subscriptionRepository.deleteById(new Subscription.Key(memberId, blog.getId()));
        return new SubscriptionResponse(false, subscriptionRepository.countByBlogId(blog.getId()));
    }

    @Transactional(readOnly = true)
    public List<SubscribedBlog> mine(Long memberId) {
        return subscriptionRepository.findSubscribedBlogs(memberId).stream()
                .map(b -> new SubscribedBlog(b.getAddress(), b.getName(), b.getDescription(), b.getProfileImageUrl()))
                .toList();
    }

    /** 블로그 화면에 보여 줄 구독자 수와 내가 구독했는지. */
    @Transactional(readOnly = true)
    public SubscriptionResponse status(Long blogId, Long viewerId) {
        boolean subscribed = viewerId != null
                && subscriptionRepository.existsById(new Subscription.Key(viewerId, blogId));
        return new SubscriptionResponse(subscribed, subscriptionRepository.countByBlogId(blogId));
    }

    public record SubscriptionResponse(boolean subscribed, long subscriberCount) {
    }

    public record SubscribedBlog(String address, String name, String description, String profileImageUrl) {
    }
}
