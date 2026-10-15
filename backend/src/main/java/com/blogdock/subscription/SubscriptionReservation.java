package com.blogdock.subscription;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** SUB-07 예약 맞구독. 블로그가 없는 회원(target)이 블로그를 만들면 구독으로 바뀌고 지워진다. */
@Entity
@Table(name = "subscription_reservation")
@IdClass(SubscriptionReservation.Key.class)
public class SubscriptionReservation {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "target_member_id")
    private Long targetMemberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SubscriptionReservation() {
    }

    public SubscriptionReservation(Long userId, Long targetMemberId) {
        this.userId = userId;
        this.targetMemberId = targetMemberId;
        this.createdAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public record Key(Long userId, Long targetMemberId) implements Serializable {
        public Key() {
            this(null, null);
        }
    }
}
