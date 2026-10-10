package com.blogdock.comment;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.BlogAccess;
import com.blogdock.common.ApiException;
import com.blogdock.post.Post;
import com.blogdock.post.PostService;

/** CMT-01 쓰기·삭제, CMT-02 주인의 삭제, CMT-03 수정. */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final BlogAccess blogAccess;

    public CommentService(CommentRepository commentRepository, PostService postService, BlogAccess blogAccess) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.blogAccess = blogAccess;
    }

    /** 작성순. 볼 수 없는 글이면 404. */
    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long postId, Long viewerId) {
        Post post = postService.findViewable(postId, viewerId);
        boolean postOwner = blogAccess.find(post.getBlogId()).isOwnedBy(viewerId);
        return commentRepository.findWithAuthor(postId).stream().map(row -> {
            Comment c = (Comment) row[0];
            boolean mine = c.isWrittenBy(viewerId);
            return new CommentResponse(c.getId(), c.getContent(), (String) row[1], (String) row[2],
                    c.getCreatedAt(), c.getUpdatedAt(), mine, mine || postOwner);
        }).toList();
    }

    @Transactional
    public CommentResponse write(Long postId, Long memberId, String content) {
        postService.findViewable(postId, memberId);
        Comment c = commentRepository.save(Comment.write(postId, memberId, content.strip()));
        return new CommentResponse(c.getId(), c.getContent(), null, null, c.getCreatedAt(), c.getUpdatedAt(),
                true, true);
    }

    /** 자기 댓글만 고친다. 글 주인도 남의 댓글은 못 고친다. */
    @Transactional
    public CommentResponse edit(Long commentId, Long memberId, String content) {
        Comment c = find(commentId);
        postService.findViewable(c.getPostId(), memberId);
        if (!c.isWrittenBy(memberId)) {
            throw ApiException.forbidden("내 댓글만 고칠 수 있어요");
        }
        c.edit(content.strip());
        return new CommentResponse(c.getId(), c.getContent(), null, null, c.getCreatedAt(), Instant.now(),
                true, true);
    }

    /** 자기 댓글, 또는 내 글에 달린 댓글을 지운다. */
    @Transactional
    public void delete(Long commentId, Long memberId) {
        Comment c = find(commentId);
        Post post = postService.findViewable(c.getPostId(), memberId);
        boolean postOwner = blogAccess.find(post.getBlogId()).isOwnedBy(memberId);
        if (!c.isWrittenBy(memberId) && !postOwner) {
            throw ApiException.forbidden("이 댓글을 지울 권한이 없어요");
        }
        commentRepository.delete(c);
    }

    private Comment find(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> ApiException.notFound("댓글을 찾을 수 없어요"));
    }

    public record CommentResponse(Long id, String content, String nickname, String profileImageUrl,
                                  Instant createdAt, Instant updatedAt, boolean canEdit, boolean canDelete) {
    }
}
