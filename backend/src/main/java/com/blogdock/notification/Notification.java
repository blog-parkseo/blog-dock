package com.blogdock.notification;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 알림 한 건. 문구에 쓸 닉네임·블로그 이름·글 제목은 만들 때 적어 둔다. */
@Entity
@Table(name = "notification")
public class Notification {

    public static final String COMMENT = "COMMENT";
    public static final String LIKE = "LIKE";
    public static final String SUBSCRIBE = "SUBSCRIBE";
    public static final String NEW_POST = "NEW_POST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false)
    private String type;

    @Column(name = "blog_id")
    private Long blogId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "blog_name")
    private String blogName;

    @Column(name = "post_title")
    private String postTitle;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public static Notification of(Long recipientId, Long actorId, String type, Long blogId, Long postId,
                                  String actorName, String blogName, String postTitle) {
        Notification n = new Notification();
        n.recipientId = recipientId;
        n.actorId = actorId;
        n.type = type;
        n.blogId = blogId;
        n.postId = postId;
        n.actorName = actorName;
        n.blogName = blogName;
        n.postTitle = postTitle;
        n.createdAt = Instant.now();
        return n;
    }

    public void read() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getType() {
        return type;
    }

    public Long getBlogId() {
        return blogId;
    }

    public Long getPostId() {
        return postId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getBlogName() {
        return blogName;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
