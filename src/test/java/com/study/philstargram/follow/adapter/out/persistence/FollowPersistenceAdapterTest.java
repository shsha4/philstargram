package com.study.philstargram.follow.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.follow.domain.Follow;
import com.study.philstargram.follow.domain.FollowRepository;
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
@Import(FollowPersistenceAdapter.class)
@Testcontainers
class FollowPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    FollowRepository followRepository;

    @Test
    void savesAndDetectsExistingFollow() {
        followRepository.save(Follow.create(1L, 2L));

        assertThat(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).isTrue();
        assertThat(followRepository.existsByFollowerIdAndFolloweeId(2L, 1L)).isFalse();
    }

    @Test
    void deletesFollowRelation() {
        followRepository.save(Follow.create(1L, 2L));

        followRepository.deleteByFollowerIdAndFolloweeId(1L, 2L);

        assertThat(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).isFalse();
    }

    @Test
    void findsFollowerIdsByFolloweeId() {
        followRepository.save(Follow.create(2L, 1L));
        followRepository.save(Follow.create(3L, 1L));
        followRepository.save(Follow.create(3L, 2L));

        assertThat(followRepository.findFollowerIdsByFolloweeId(1L)).containsExactlyInAnyOrder(2L, 3L);
    }
}
