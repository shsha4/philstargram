package com.study.philstargram.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.member.domain.Member;
import com.study.philstargram.member.domain.MemberId;
import com.study.philstargram.member.domain.MemberRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMemberUseCaseTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    GetMemberUseCase getMemberUseCase;

    @Test
    void returnsMemberWhenFound() {
        Member member = Member.reconstitute(1L, "phill@example.com", "phill", "hello", LocalDateTime.now());
        when(memberRepository.findById(MemberId.of(1L))).thenReturn(Optional.of(member));

        MemberResult result = getMemberUseCase.execute(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("phill");
    }

    @Test
    void throwsWhenMemberNotFound() {
        when(memberRepository.findById(MemberId.of(1L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMemberUseCase.execute(1L)).isInstanceOf(NotFoundException.class);
    }
}
