package com.blogdock.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSocialRepository extends JpaRepository<MemberSocial, Long> {

    Optional<MemberSocial> findByProviderAndSocialId(String provider, String socialId);

    Optional<MemberSocial> findByMemberIdAndProvider(Long memberId, String provider);

    List<MemberSocial> findByMemberIdOrderByCreatedAtAsc(Long memberId);

    long countByMemberId(Long memberId);
}
