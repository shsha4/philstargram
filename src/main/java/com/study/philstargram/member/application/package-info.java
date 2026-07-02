/**
 * member 모듈의 공개 API. 이 패키지의 타입들(UseCase, QueryService, command/result)은
 * 다른 모듈이 의존해도 되지만, {@code domain} 과 {@code adapter} 는 내부 구현으로 유지한다.
 */
@org.springframework.modulith.NamedInterface("application")
package com.study.philstargram.member.application;
