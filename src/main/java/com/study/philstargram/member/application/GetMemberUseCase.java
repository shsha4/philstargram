package com.study.philstargram.member.application;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMemberUseCase {

    private final MemberRepository memberRepository;

    public GetMemberUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public MemberResult execute(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResult::from)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다: " + memberId));
    }
}
