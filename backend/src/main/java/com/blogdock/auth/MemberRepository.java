package com.blogdock.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findBySocialProviderAndSocialId(String socialProvider, String socialId);

    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
