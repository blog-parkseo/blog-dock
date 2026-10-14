package com.blogdock.notification;

/**
 * 알림을 만들 일. 댓글·공감·구독·발행이 DB에 저장된 뒤(커밋 뒤)에 알림을 만든다.
 * 알림 만들기가 실패해도 댓글·공감 같은 원래 일은 그대로 남는다.
 */
public final class NotificationEvents {

    private NotificationEvents() {
    }

    /** 답글이면 parentAuthorId에 원래 댓글 쓴 사람이 들어간다 (아니면 null). */
    public record CommentWritten(Long postId, Long actorId, Long parentAuthorId) {

        public CommentWritten(Long postId, Long actorId) {
            this(postId, actorId, null);
        }
    }

    public record PostLiked(Long postId, Long actorId) {
    }

    public record BlogSubscribed(Long blogId, Long actorId) {
    }

    /** SUB-07 예약 맞구독이 이루어졌다: 예약한 사람(userId)이 새 블로그(blogId)를 구독하게 됐다. */
    public record ReservationFulfilled(Long blogId, Long userId) {
    }

    /** 처음 발행했을 때만. 비공개 글은 보내지 않는다. */
    public record PostPublished(Long postId) {
    }
}
