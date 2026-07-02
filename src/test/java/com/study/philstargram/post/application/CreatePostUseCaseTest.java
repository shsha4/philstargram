package com.study.philstargram.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.post.domain.Post;
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
        when(memberQueryService.existsById(1L)).thenReturn(true);
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> {
                    Post post = invocation.getArgument(0);
                    return Post.reconstitute(10L, post.getAuthorId(), post.getContent(), post.getCreatedAt());
                });

        PostResult result = createPostUseCase.execute(new CreatePostCommand(1L, "hello world"));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.content()).isEqualTo("hello world");
        verify(eventPublisher).publishEvent(any(PostCreatedEvent.class));
    }

    @Test
    void rejectsWhenAuthorDoesNotExist() {
        when(memberQueryService.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> createPostUseCase.execute(new CreatePostCommand(1L, "hello world")))
                .isInstanceOf(NotFoundException.class);
    }
}
