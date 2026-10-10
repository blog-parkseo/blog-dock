package com.blogdock.tag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    /** 대소문자만 다른 이름은 같은 태그다 (MySQL 기본 정렬 규칙과 맞춘다). */
    Optional<Tag> findFirstByBlogIdAndNameIgnoreCase(Long blogId, String name);

    /** 글에 달린 태그 이름 (단 순서대로). */
    @Query("select t.name from PostTag pt join Tag t on t.id = pt.tagId where pt.postId = :postId order by pt.createdAt, t.id")
    List<String> namesOfPost(Long postId);

    /** TAG-03: 보는 사람이 볼 수 있는 글에 달린 태그만, 글 수와 함께. */
    @Query("""
            select t.name, count(p) from Tag t
              join PostTag pt on pt.tagId = t.id
              join Post p on p.id = pt.postId
            where t.blogId = :blogId and p.status = 'PUBLISHED' and (:owner = true or p.visibility = 'PUBLIC')
            group by t.name
            order by t.name""")
    List<Object[]> countsOfBlog(Long blogId, boolean owner);

    @Modifying
    @Query("delete from PostTag pt where pt.postId = :postId")
    void unlinkAll(Long postId);
}
