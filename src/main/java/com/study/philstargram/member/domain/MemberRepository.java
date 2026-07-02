package com.study.philstargram.member.domain;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    boolean existsById(Long id);

    boolean existsByEmail(String email);
}
