package com.study.philstargram.member.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    boolean existsByEmail(String email);
}
