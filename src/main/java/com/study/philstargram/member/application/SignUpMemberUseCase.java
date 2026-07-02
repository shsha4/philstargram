package com.study.philstargram.member.application;

import com.study.philstargram.common.exception.DuplicateException;
import com.study.philstargram.member.domain.Member;
import com.study.philstargram.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignUpMemberUseCase {

    private final MemberRepository memberRepository;

    public SignUpMemberUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberResult execute(SignUpMemberCommand command) {
        if (memberRepository.existsByEmail(command.email())) {
            throw new DuplicateException("이미 가입된 이메일입니다: " + command.email());
        }
        Member member = Member.signUp(command.email(), command.nickname(), command.bio());
        return MemberResult.from(memberRepository.save(member));
    }
}
