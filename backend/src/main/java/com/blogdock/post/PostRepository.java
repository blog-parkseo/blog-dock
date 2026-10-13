package com.blogdock.post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 목록은 모두 "처음 발행한 시각 최신순, 같으면 나중에 만든 글 먼저"다.
 * owner=false이면 비공개 글이 빠진다 (공통 정책 2).
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByBlogIdAndSlug(Long blogId, String slug);

    /** BLOG-03 블로그 메인, CAT-02 카테고리별, TAG-02 태그별, SRCH-01 검색을 한 쿼리로. 조건은 null이면 안 쓴다. */
    @Query("""
            select p from Post p
            where p.blogId = :blogId and p.status = 'PUBLISHED'
              and (:owner = true or p.visibility = 'PUBLIC')
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:uncategorized = false or p.categoryId is null)
              and (:tag is null or p.id in (
                    select pt.postId from PostTag pt join Tag t on t.id = pt.tagId
                    where t.blogId = :blogId and lower(t.name) = lower(:tag)))
              and (:q is null
                    or lower(p.title) like :q escape '!'
                    or lower(p.contentText) like :q escape '!'
                    or p.id in (
                       select pt2.postId from PostTag pt2 join Tag t2 on t2.id = pt2.tagId
                       where t2.blogId = :blogId and lower(t2.name) like :q escape '!'))
            order by p.publishedAt desc, p.id desc""")
    Page<Post> findBlogPosts(Long blogId, boolean owner, Long categoryId, boolean uncategorized,
                             String tag, String q, Pageable pageable);

    /** HOME-01: 모든 블로그의 공개 글. 커서(마지막으로 본 글의 발행 시각과 id) 다음부터. */
    @Query("""
            select p from Post p
            where p.status = 'PUBLISHED' and p.visibility = 'PUBLIC'
              and (:cursorAt is null or p.publishedAt < :cursorAt
                   or (p.publishedAt = :cursorAt and p.id < :cursorId))
            order by p.publishedAt desc, p.id desc""")
    List<Post> findHome(Instant cursorAt, Long cursorId, Pageable pageable);

    /** SUB-03 구독 피드: 내가 구독한 블로그의 공개 글. 홈과 같은 커서 방식. */
    @Query("""
            select p from Post p
            where p.status = 'PUBLISHED' and p.visibility = 'PUBLIC'
              and p.blogId in (select s.blogId from Subscription s where s.userId = :memberId)
              and (:cursorAt is null or p.publishedAt < :cursorAt
                   or (p.publishedAt = :cursorAt and p.id < :cursorId))
            order by p.publishedAt desc, p.id desc""")
    List<Post> findFeed(Long memberId, Instant cursorAt, Long cursorId, Pageable pageable);

    /** HOME-02 인기 글: since 이후 받은 공감 수 [글 id, 개수]. 공개·발행 글만. */
    @Query("""
            select l.postId, count(l) from PostLike l, Post p
            where p.id = l.postId and l.createdAt >= :since
              and p.status = 'PUBLISHED' and p.visibility = 'PUBLIC'
            group by l.postId""")
    List<Object[]> countRecentLikes(Instant since);

    /** HOME-02 인기 글: since 이후 받은 댓글 수 [글 id, 개수]. 글쓴이 본인 댓글은 빼고 센다. */
    @Query("""
            select c.postId, count(c) from Comment c, Post p, Blog b
            where p.id = c.postId and b.id = p.blogId and c.createdAt >= :since
              and c.authorId <> b.ownerId
              and p.status = 'PUBLISHED' and p.visibility = 'PUBLIC'
            group by c.postId""")
    List<Object[]> countRecentComments(Instant since);

    /** BLOG-04 사이드바: 카테고리별 글 수 (카테고리 없음 = null). */
    @Query("""
            select p.categoryId, count(p) from Post p
            where p.blogId = :blogId and p.status = 'PUBLISHED' and (:owner = true or p.visibility = 'PUBLIC')
            group by p.categoryId""")
    List<Object[]> countByCategory(Long blogId, boolean owner);

    /** POST-10 이전 글 (더 오래된 글). */
    @Query("""
            select p from Post p
            where p.blogId = :blogId and p.status = 'PUBLISHED' and (:owner = true or p.visibility = 'PUBLIC')
              and (p.publishedAt < :at or (p.publishedAt = :at and p.id < :id))
            order by p.publishedAt desc, p.id desc""")
    List<Post> findOlder(Long blogId, boolean owner, Instant at, Long id, Pageable one);

    /** POST-10 다음 글 (더 새 글). */
    @Query("""
            select p from Post p
            where p.blogId = :blogId and p.status = 'PUBLISHED' and (:owner = true or p.visibility = 'PUBLIC')
              and (p.publishedAt > :at or (p.publishedAt = :at and p.id > :id))
            order by p.publishedAt asc, p.id asc""")
    List<Post> findNewer(Long blogId, boolean owner, Instant at, Long id, Pageable one);

    /** MNG-01 탭별 글 수: [상태, 공개 범위, 개수]. */
    @Query("select p.status, p.visibility, count(p) from Post p where p.blogId = :blogId group by p.status, p.visibility")
    List<Object[]> countByStatus(Long blogId);

    /** MNG-01 임시저장 글. 최근에 고친 순. */
    Page<Post> findByBlogIdAndStatusOrderByUpdatedAtDescIdDesc(Long blogId, String status, Pageable pageable);

    /** MNG-01 발행 글 (공개/비공개). */
    Page<Post> findByBlogIdAndStatusAndVisibilityOrderByPublishedAtDescIdDesc(
            Long blogId, String status, String visibility, Pageable pageable);
}
