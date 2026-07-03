package com.study.philstargram.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.member.application.MemberSummary;
import com.study.philstargram.post.domain.Post;
import com.study.philstargram.post.domain.PostId;
import com.study.philstargram.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreatePostUseCaseTest {

    @Mock
    PostRepository postRepository;

    @Mock
    MemberQueryService memberQueryService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    CreatePostUseCase createPostUseCase;

    @Test
    void createsPostWhenAuthorExists() {
        // 존재 검증 + 닉네임 확보를 getSummary 한 번으로 한다(event-carried state).
        when(memberQueryService.getSummary(1L)).thenReturn(new MemberSummary(1L, "alice"));
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> {
                    // 실제 어댑터처럼 DB 가 생성한 식별자를 같은 인스턴스에 부여하고 그대로 반환한다
                    // (도메인 이벤트가 저장 이후에도 살아남아야 하므로).
                    Post post = invocation.getArgument(0);
                    post.assignId(PostId.of(10L));
                    return post;
                });

        PostResult result = createPostUseCase.execute(new CreatePostCommand(1L, "hello world"));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.content()).isEqualTo("hello world");
        verify(eventPublisher).publishEvent(any(PostCreatedEvent.class));
    }

    @Test
    void rejectsWhenAuthorDoesNotExist() {
        when(memberQueryService.getSummary(1L)).thenThrow(new NotFoundException("존재하지 않는 회원입니다: 1"));

        assertThatThrownBy(() -> createPostUseCase.execute(new CreatePostCommand(1L, "hello world")))
                .isInstanceOf(NotFoundException.class);
    }
}
