package com.study.philstargram.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NicknameTest {

    @Test
    void acceptsNicknameWithinLength() {
        assertThat(Nickname.of("phill").value()).isEqualTo("phill");
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> Nickname.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooShort() {
        assertThatThrownBy(() -> Nickname.of("a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooLong() {
        assertThatThrownBy(() -> Nickname.of("a".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
