package com.study.philstargram.member.application;

/**
 * 모듈 간 최소한의 조회 계약(contract). 다른 모듈은
 * {@link com.study.philstargram.member.domain.Member} 나 {@link MemberResult} 대신 이
 * 타입에 의존해야 하며, 그래야 member 의 내부 프로필 필드를 자유롭게 발전시킬 수 있다.
 */
public record MemberSummary(Long id, String nickname) {
}
