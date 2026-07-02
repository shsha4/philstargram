package com.study.philstargram;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * 직접 작성한 {@link ArchitectureTest} 의 경계 규칙을, Spring Modulith 자체의 모듈 모델과
 * 교차 검증한다. Spring Modulith 는 이 클래스가 속한 패키지의 직속 하위 패키지
 * (member, post, follow, feed, common)로부터 모듈을 추론한다.
 */
class ModularityTest {

    ApplicationModules modules = ApplicationModules.of(PhilstargramApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
