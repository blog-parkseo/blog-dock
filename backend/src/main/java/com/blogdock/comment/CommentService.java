package com.blogdock.comment;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogAccess;
import com.blogdock.common.ApiException;
import com.blogdock.notification.NotificationEvents.CommentWritten;
import com.blogdock.post.Post;
import com.blogdock.post.PostService;

/** CMT-01 쓰기·삭제, CMT-02 주인의 삭제, CMT-03 수정, 비밀 댓글, 답글. */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final BlogAccess blogAccess;
    private final ApplicationEventPublisher events;

    public CommentService(CommentRepository commentRepository, PostService postService, BlogAccess blogAccess,
                          ApplicationEventPublisher events) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.blogAccess = blogAccess;
        this.events = events;
    }

    /**
     * 작성순. 볼 수 없는 글이면 404. 답글은 parentId로 원래 댓글을 가리킨다 (화면에서 묶어 보여 준다).
     * 비밀 댓글은 글 주인과 쓴 사람만 본다. 비밀 댓글에 달린 답글이나 비밀 답글은 원래 댓글 쓴 사람도 본다.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long postId, Long viewerId) {
        Post post = postService.findViewable(postId, viewerId);
        Blog blog = blogAccess.find(post.getBlogId());
        boolean postOwner = blog.isOwnedBy(viewerId);
        List<Object[]> rows = commentRepository.findWithAuthor(postId);
        Map<Long, Comment> byId = new HashMap<>();
        rows.forEach(row -> byId.put(((Comment) row[0]).getId(), (Comment) row[0]));
        return rows.stream().map(row -> {
            Comment c = (Comment) row[0];
            Comment parent = c.isReply() ? byId.get(c.getParentId()) : null;
            boolean mine = c.isWrittenBy(viewerId);
            boolean secret = c.isSecret() || (parent != null && parent.isSecret());
            boolean canSee = !secret || mine || postOwner || (parent != null && parent.isWrittenBy(viewerId));
            if (!canSee) {
                return new CommentResponse(c.getId(), c.getParentId(), null, null, null, c.getCreatedAt(),
                        c.getUpdatedAt(), false, false, true, false);
            }
            return new CommentResponse(c.getId(), c.getParentId(), c.getContent(), (String) row[1], (String) row[2],
                    c.getCreatedAt(), c.getUpdatedAt(), mine, mine || postOwner, secret,
                    blog.isOwnedBy(c.getAuthorId()));
        }).toList();
    }

    @Transactional
    public CommentResponse write(Long postId, Long memberId, String content, boolean secret) {
        postService.findViewable(postId, memberId);
        Comment c = commentRepository.save(Comment.write(postId, memberId, content.strip(), secret));
        events.publishEvent(new CommentWritten(postId, memberId));
        return written(c, postId, memberId);
    }

    /**
     * 답글. 로그인한 누구나 단다. 답글에 다시 답글을 달면 원래 댓글 밑에 붙는다 (한 단계만).
     * 내가 볼 수 없는 비밀 댓글에는 못 달고, 비밀 댓글에 단 답글은 늘 비밀이다.
     */
    @Transactional
    public CommentResponse reply(Long parentId, Long memberId, String content, boolean secret) {
        Comment parent = find(parentId);
        if (parent.isReply()) {
            parent = find(parent.getParentId());
        }
        Post post = postService.findViewable(parent.getPostId(), memberId);
        boolean postOwner = blogAccess.find(post.getBlogId()).isOwnedBy(memberId);
        if (parent.isSecret() && !parent.isWrittenBy(memberId) && !postOwner) {
            throw ApiException.forbidden("비밀 댓글에는 답글을 달 수 없어요");
        }
        Comment c = commentRepository.save(Comment.reply(parent, memberId, content.strip(),
                secret || parent.isSecret()));
        events.publishEvent(new CommentWritten(post.getId(), memberId, parent.getAuthorId()));
        return written(c, post.getId(), memberId);
    }

    private CommentResponse written(Comment c, Long postId, Long memberId) {
        Post post = postService.findViewable(postId, memberId);
        return new CommentResponse(c.getId(), c.getParentId(), c.getContent(), null, null, c.getCreatedAt(),
                c.getUpdatedAt(), true, true, c.isSecret(), blogAccess.find(post.getBlogId()).isOwnedBy(memberId));
    }

    /** 자기 댓글만 고친다. 글 주인도 남의 댓글은 못 고친다. secret을 안 보내면 비밀 여부는 그대로 둔다. */
    @Transactional
    public CommentResponse edit(Long commentId, Long memberId, String content, Boolean secret) {
        Comment c = find(commentId);
        postService.findViewable(c.getPostId(), memberId);
        if (!c.isWrittenBy(memberId)) {
            throw ApiException.forbidden("내 댓글만 고칠 수 있어요");
        }
        boolean keepSecret = c.isReply() && find(c.getParentId()).isSecret(); // 비밀 댓글의 답글은 늘 비밀
        c.edit(content.strip(), keepSecret || (secret == null ? c.isSecret() : secret));
        return new CommentResponse(c.getId(), c.getParentId(), c.getContent(), null, null, c.getCreatedAt(),
                Instant.now(), true, true, c.isSecret(), false);
    }

    /** 자기 댓글, 또는 내 글에 달린 댓글을 지운다. 원래 댓글을 지우면 답글도 같이 지워진다 (DB가 지운다). */
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

    /** byPostOwner: 블로그 주인이 쓴 댓글이면 true (화면에 "작성자" 표시). */
    public record CommentResponse(Long id, Long parentId, String content, String nickname, String profileImageUrl,
                                  Instant createdAt, Instant updatedAt, boolean canEdit, boolean canDelete,
                                  boolean secret, boolean byPostOwner) {
    }
}
