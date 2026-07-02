/**
 * 팔로우 그래프(팔로워/팔로위 관계)를 소유하는 모듈. 팔로우 관계는 프로필 데이터와는
 * 변경되는 이유가 다르기 때문에 {@code member} 와 분리한다. member 에 대해서는
 * {@link com.study.philstargram.member.application.MemberQueryService} 를 통해서만
 * 의존한다. {@link com.study.philstargram.follow.application.FollowQueryService} 를
 * 다른 모듈(feed 등)을 위한 공개 조회 API 로 노출한다.
 */
package com.study.philstargram.follow;
