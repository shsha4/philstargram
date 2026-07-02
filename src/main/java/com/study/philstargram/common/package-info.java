/**
 * 공유 커널(shared kernel): 예외, {@code ApiResponse} 응답 래퍼, 전역 예외 핸들러를 담는다.
 * member/post/follow/feed 와 달리 이 모듈은 보호해야 할 자체 비즈니스 규칙이 없고 모든
 * 다른 모듈이 하위 패키지 전부에 의존할 것으로 예상되므로 {@code OPEN} 으로 표시한다.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.study.philstargram.common;
