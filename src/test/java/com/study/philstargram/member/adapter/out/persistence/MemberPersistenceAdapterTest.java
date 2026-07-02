package com.study.philstargram.member.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.member.domain.Member;
import com.study.philstargram.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MemberPersistenceAdapter.class)
@Testcontainers
class MemberPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    MemberRepository memberRepository;

    @Test
    void savesAndFindsMemberById() {
        Member saved = memberRepository.save(Member.signUp("phill@example.com", "phill", "hello"));

        var found = memberRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("phill@example.com");
    }

    @Test
    void existsByEmailReflectsSavedMembers() {
        memberRepository.save(Member.signUp("dup@example.com", "dup", null));

        assertThat(memberRepository.existsByEmail("dup@example.com")).isTrue();
        assertThat(memberRepository.existsByEmail("nobody@example.com")).isFalse();
    }
}
