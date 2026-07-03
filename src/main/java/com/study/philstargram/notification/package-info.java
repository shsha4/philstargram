/**
 * 다른 모듈의 이벤트에 반응하여 알림을 생성하는 모듈. 자체 생명주기는 소유하지 않는다.
 * Kafka 토픽 {@code post.created}(팔로우한 사람의 새 게시글)와 {@code member.followed}(누군가
 * 나를 팔로우함)를 구독한다.
 *
 * <p>phase 4 결합 제거: 알림 문구에 필요한 닉네임을 이벤트가 실어오므로(event-carried state)
 * member 를 동기 호출하지 않는다. 팔로워 목록이 필요한 새 게시글 알림만 follow 를 조회한다.
 */
package com.study.philstargram.notification;
