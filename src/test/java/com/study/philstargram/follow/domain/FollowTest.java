package com.study.philstargram.follow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FollowTest {

    @Test
    void createsFollowBetweenTwoMembers() {
        Follow follow = Follow.create(1L, 2L);

        assertThat(follow.getFollowerId()).isEqualTo(1L);
        assertThat(follow.getFolloweeId()).isEqualTo(2L);
    }

    @Test
    void rejectsSelfFollow() {
        assertThatThrownBy(() -> Follow.create(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
