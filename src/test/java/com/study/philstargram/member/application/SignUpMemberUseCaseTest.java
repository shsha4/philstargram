package com.study.philstargram.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.common.exception.DuplicateException;
import com.study.philstargram.member.domain.Member;
import com.study.philstargram.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignUpMemberUseCaseTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    SignUpMemberUseCase signUpMemberUseCase;

    @Test
    void signsUpWhenEmailIsNotTaken() {
        when(memberRepository.existsByEmail("phill@example.com")).thenReturn(false);
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.<Member>getArgument(0).withId(1L));

        MemberResult result = signUpMemberUseCase.execute(new SignUpMemberCommand("phill@example.com", "phill", "hello"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("phill@example.com");
        assertThat(result.nickname()).isEqualTo("phill");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(memberRepository.existsByEmail("phill@example.com")).thenReturn(true);

        assertThatThrownBy(() -> signUpMemberUseCase.execute(new SignUpMemberCommand("phill@example.com", "phill", "hello")))
                .isInstanceOf(DuplicateException.class);
    }
}
