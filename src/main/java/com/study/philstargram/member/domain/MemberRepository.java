package com.study.philstargram.member.domain;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(MemberId id);

    boolean existsById(MemberId id);

    boolean existsByEmail(String email);
}
