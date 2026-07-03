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

    @Test
    void raisesMemberFollowedEventOnCreate() {
        Follow follow = Follow.create(1L, 2L);

        assertThat(follow.pullDomainEvents())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.followerId()).isEqualTo(1L);
                    assertThat(event.followeeId()).isEqualTo(2L);
                });
    }

    @Test
    void drainsDomainEventsOnlyOnce() {
        Follow follow = Follow.create(1L, 2L);

        assertThat(follow.pullDomainEvents()).hasSize(1);
        assertThat(follow.pullDomainEvents()).isEmpty();
    }

    @Test
    void reconstitutedFollowRaisesNoEvents() {
        Follow follow = Follow.reconstitute(5L, 1L, 2L, java.time.LocalDateTime.now());

        assertThat(follow.pullDomainEvents()).isEmpty();
    }
}
