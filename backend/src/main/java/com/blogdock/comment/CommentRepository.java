package com.blogdock.comment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByPostId(Long postId);

    /** 작성순. 닉네임을 같이 가져온다: [Comment, nickname, profileImageUrl] */
    @Query("""
            select c, m.nickname, m.profileImageUrl from Comment c join Member m on m.id = c.authorId
            where c.postId = :postId
            order by c.createdAt asc, c.id asc""")
    List<Object[]> findWithAuthor(Long postId);
}
