package com.study.philstargram.post.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.post.domain.Post;
import com.study.philstargram.post.domain.PostRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostPersistenceAdapter.class)
@Testcontainers
class PostPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    PostRepository postRepository;

    @Test
    void savesAndFindsPostById() {
        Post saved = postRepository.save(Post.write(1L, "alice", "hello world"));

        var found = postRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("hello world");
        assertThat(found.get().getAuthorNickname()).isEqualTo("alice");
    }

    @Test
    void 여러_작성자의_최근_글을_최신순으로_모은다() {
        // 읽기 시점 pull(하이브리드 팬아웃, phase 5c): 셀럽 작성자들의 최근 글을 최신순으로.
        postRepository.save(Post.write(2L, "alice", "a-old"));
        postRepository.save(Post.write(3L, "bob", "b-new"));
        postRepository.save(Post.write(9L, "carol", "not-followed"));

        List<Post> recent = postRepository.findRecentByAuthorIds(List.of(2L, 3L), 20);

        assertThat(recent).extracting(Post::getContent).containsExactly("b-new", "a-old");
    }

    @Test
    void 빈_작성자_목록이면_빈_결과를_반환한다() {
        assertThat(postRepository.findRecentByAuthorIds(List.of(), 20)).isEmpty();
    }
}
