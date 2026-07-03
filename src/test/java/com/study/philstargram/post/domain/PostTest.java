package com.study.philstargram.post.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PostTest {

    @Test
    void writesPostWithinLengthLimit() {
        Post post = Post.write(1L, "hello");

        assertThat(post.getAuthorId()).isEqualTo(1L);
        assertThat(post.getContent()).isEqualTo("hello");
    }

    @Test
    void rejectsBlankContent() {
        assertThatThrownBy(() -> Post.write(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsContentOverMaxLength() {
        String tooLong = "a".repeat(Post.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> Post.write(1L, tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsContentExactlyAtMaxLength() {
        String atLimit = "a".repeat(Post.MAX_CONTENT_LENGTH);

        assertThat(Post.write(1L, atLimit).getContent()).hasSize(Post.MAX_CONTENT_LENGTH);
    }

    @Test
    void raisesPostWrittenEventAfterIdIsAssigned() {
        Post post = Post.write(1L, "hello");
        post.assignId(PostId.of(10L));

        assertThat(post.pullDomainEvents())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.postId()).isEqualTo(PostId.of(10L));
                    assertThat(event.authorId()).isEqualTo(1L);
                    assertThat(event.content()).isEqualTo("hello");
                });
    }

    @Test
    void drainsDomainEventsOnlyOnce() {
        Post post = Post.write(1L, "hello");
        post.assignId(PostId.of(10L));

        assertThat(post.pullDomainEvents()).hasSize(1);
        assertThat(post.pullDomainEvents()).isEmpty();
    }

    @Test
    void reconstitutedPostRaisesNoEvents() {
        Post post = Post.reconstitute(10L, 1L, "hello", java.time.LocalDateTime.now());

        assertThat(post.pullDomainEvents()).isEmpty();
    }

    @Test
    void rejectsAssigningIdTwice() {
        Post post = Post.write(1L, "hello");
        post.assignId(PostId.of(10L));

        assertThatThrownBy(() -> post.assignId(PostId.of(11L)))
                .isInstanceOf(IllegalStateException.class);
    }
}
