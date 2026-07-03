package com.study.philstargram.member.adapter.out.persistence;

import com.study.philstargram.member.domain.Member;
import com.study.philstargram.member.domain.MemberId;
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
    public Optional<Member> findById(MemberId id) {
        return memberJpaRepository.findById(id.value()).map(MemberPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsById(MemberId id) {
        return memberJpaRepository.existsById(id.value());
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberJpaRepository.existsByEmail(email);
    }

    private static MemberJpaEntity toEntity(Member member) {
        Long id = member.getId() == null ? null : member.getId().value();
        return new MemberJpaEntity(id, member.getEmail().value(), member.getNickname().value(), member.getBio(), member.getCreatedAt());
    }

    private static Member toDomain(MemberJpaEntity entity) {
        return Member.reconstitute(entity.getId(), entity.getEmail(), entity.getNickname(), entity.getBio(), entity.getCreatedAt());
    }
}
