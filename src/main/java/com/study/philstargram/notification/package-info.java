/**
 * 다른 모듈의 이벤트에 반응하여 알림을 생성하는 모듈. 자체 생명주기는 소유하지 않는다.
 * {@code post} 의 {@code PostCreatedEvent}(팔로우한 사람의 새 게시글)와 {@code follow} 의
 * {@code MemberFollowedEvent}(누군가 나를 팔로우함)를 구독한다. member 에 대해서는
 * {@link com.study.philstargram.member.application.MemberQueryService} 를 통해서만
 * 의존한다.
 */
package com.study.philstargram.notification;
