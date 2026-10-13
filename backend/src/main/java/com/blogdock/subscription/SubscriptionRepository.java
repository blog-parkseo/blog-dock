package com.blogdock.subscription;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.blogdock.blog.Blog;

public interface SubscriptionRepository extends JpaRepository<Subscription, Subscription.Key> {

    long countByBlogId(Long blogId);

    /** HOME-04: since 이후 블로그별 새 구독자 수 [블로그 id, 개수]. */
    @Query("select s.blogId, count(s) from Subscription s where s.createdAt >= :since group by s.blogId")
    List<Object[]> countRecentByBlog(Instant since);

    /** HOME-04 동점 처리용: 블로그별 전체 구독자 수. */
    @Query("select s.blogId, count(s) from Subscription s where s.blogId in :blogIds group by s.blogId")
    List<Object[]> countByBlogIds(Collection<Long> blogIds);

    /** 이 블로그를 구독한 회원들 (SUB-05 맞구독 판단). */
    @Query("select s.userId from Subscription s where s.blogId = :blogId")
    List<Long> findUserIdsByBlogId(Long blogId);

    /** SUB-02 내가 구독한 블로그. 최근에 구독한 순. */
    @Query("""
            select b from Subscription s join Blog b on b.id = s.blogId
            where s.userId = :userId
            order by s.createdAt desc, b.id desc""")
    List<Blog> findSubscribedBlogs(Long userId);
}
