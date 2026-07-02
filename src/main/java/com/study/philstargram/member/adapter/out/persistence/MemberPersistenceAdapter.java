package com.study.philstargram.member.adapter.out.persistence;

import com.study.philstargram.member.domain.Member;
import com.study.philstargram.member.domain.MemberRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class MemberPersistenceAdapter implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    MemberPersistenceAdapter(MemberJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public Member save(Member member) {
        MemberJpaEntity saved = memberJpaRepository.save(toEntity(member));
        return toDomain(saved);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id).map(MemberPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return memberJpaRepository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberJpaRepository.existsByEmail(email);
    }

    private static MemberJpaEntity toEntity(Member member) {
        return new MemberJpaEntity(member.getId(), member.getEmail(), member.getNickname(), member.getBio(), member.getCreatedAt());
    }

    private static Member toDomain(MemberJpaEntity entity) {
        return Member.reconstitute(entity.getId(), entity.getEmail(), entity.getNickname(), entity.getBio(), entity.getCreatedAt());
    }
}
