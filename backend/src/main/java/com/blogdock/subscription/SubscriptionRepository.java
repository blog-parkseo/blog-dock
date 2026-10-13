package com.blogdock.subscription;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.blogdock.blog.Blog;

public interface SubscriptionRepository extends JpaRepository<Subscription, Subscription.Key> {

    long countByBlogId(Long blogId);

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
