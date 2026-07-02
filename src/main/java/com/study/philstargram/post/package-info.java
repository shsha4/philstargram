/**
 * 게시글의 생명주기(생성/조회)를 소유하는 모듈. member 에 대해서는
 * {@link com.study.philstargram.member.application.MemberQueryService} 를 통해서만
 * 의존한다. 게시글 생성 시 {@link com.study.philstargram.post.application.PostCreatedEvent}
 * 를 발행할 뿐, 누가 그 이벤트에 반응하는지(feed, notification, search)는 알지 못한다.
 */
package com.study.philstargram.post;
