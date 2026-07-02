/**
 * 회원의 신원(identity)과 프로필 데이터를 소유하는 모듈. 다른 모듈은
 * {@code member.domain} 이나 {@code member.adapter} 를 직접 참조해서는 안 되며,
 * 모듈 간 조회는 {@link com.study.philstargram.member.application.MemberQueryService}
 * 를 통해서만 이루어진다.
 */
package com.study.philstargram.member;
