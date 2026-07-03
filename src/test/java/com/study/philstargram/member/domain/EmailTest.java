package com.study.philstargram.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void acceptsWellFormedEmail() {
        assertThat(Email.of("phill@example.com").value()).isEqualTo("phill@example.com");
    }

    @Test
    void rejectsMalformedEmail() {
        assertThatThrownBy(() -> Email.of("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void hasValueEquality() {
        assertThat(Email.of("a@b.com")).isEqualTo(Email.of("a@b.com"));
    }
}
