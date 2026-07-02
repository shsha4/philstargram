/**
 * 게시글 저장소가 아니라, 사용자별 타임라인을 담는 읽기 모델(read model). 쓰기 시점
 * 팬아웃(fan-out-on-write)으로 구성된다.
 * {@link com.study.philstargram.feed.application.FeedFanOutOnPostCreated} 가
 * {@code post} 의 {@code PostCreatedEvent} 에 반응하여, 작성자를 팔로우하는 모든
 * 사용자의 피드에 {@link com.study.philstargram.feed.domain.FeedEntry}(작성자 닉네임과
 * 본문 미리보기를 비정규화하여 포함)를 저장한다. 조회(`GetMyFeedUseCase`)는 feed 자신의
 * 테이블만 읽으므로, 조회 시점에 다른 모듈을 호출하지 않는다. 팔로워가 매우 많은 계정에
 * 대한 대안(하이브리드/읽기 시점 팬아웃)은 로드맵 phase 5 에서 검토한다.
 */
package com.study.philstargram.feed;
