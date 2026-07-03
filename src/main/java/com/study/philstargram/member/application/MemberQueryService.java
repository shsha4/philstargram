package com.study.philstargram.member.application;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.member.domain.Member;
import com.study.philstargram.member.domain.MemberId;
import com.study.philstargram.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 모듈(post, follow, feed 등)에 노출하는 공개 조회 API. 다른 모듈이 회원 정보를 얻는
 * 유일하게 허용된 방법이며, {@code member.domain} 이나
 * {@code member.adapter.out.persistence} 를 직접 참조해서는 안 된다.
 *
 * <p>모듈 경계에서는 raw {@code Long} 을 받고, 내부(도메인 포트)로 넘길 때 타입드 ID 로 감싼다.
 */
@Service
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public MemberQueryService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long memberId) {
        return memberRepository.existsById(MemberId.of(memberId));
    }

    @Transactional(readOnly = true)
    public MemberSummary getSummary(Long memberId) {
        Member member = memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다: " + memberId));
        return new MemberSummary(member.getId().value(), member.getNickname().value());
    }
}
