package com.blogdock.subscription;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionReservationRepository
        extends JpaRepository<SubscriptionReservation, SubscriptionReservation.Key> {

    List<SubscriptionReservation> findByTargetMemberId(Long targetMemberId);
}
