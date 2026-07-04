package com.study.philstargram.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.post.domain.Post;
import com.study.philstargram.post.domain.PostId;
import com.study.philstargram.post.domain.PostRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPostUseCaseTest {

    @Mock
    PostRepository postRepository;

    @InjectMocks
    GetPostUseCase getPostUseCase;

    @Test
    void returnsPostWhenFound() {
        Post post = Post.reconstitute(1L, 2L, "author", "hello", LocalDateTime.now());
        when(postRepository.findById(PostId.of(1L))).thenReturn(Optional.of(post));

        PostResult result = getPostUseCase.execute(1L);

        assertThat(result.authorId()).isEqualTo(2L);
        assertThat(result.content()).isEqualTo("hello");
    }

    @Test
    void throwsWhenPostNotFound() {
        when(postRepository.findById(PostId.of(1L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getPostUseCase.execute(1L)).isInstanceOf(NotFoundException.class);
    }
}
