package com.study.philstargram.post.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.post.domain.Post;
import com.study.philstargram.post.domain.PostRepository;
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
        Post saved = postRepository.save(Post.write(1L, "hello world"));

        var found = postRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("hello world");
    }
}
