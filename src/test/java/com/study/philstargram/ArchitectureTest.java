package com.study.philstargram;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

/**
 * 프로젝트 설계의 모듈러 모놀리스 경계 규칙을 강제한다: 어떤 모듈도 다른 모듈의
 * domain/adapter 내부에 접근할 수 없고, domain 은 프레임워크에 의존하지 않으며,
 * 모듈 간에 순환 의존이 없어야 한다.
 */
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.study.philstargram";
    private static final JavaClasses CLASSES = new ClassFileImporter().importPackages(BASE_PACKAGE);
    private static final String[] MODULES = {"member", "post", "follow", "feed", "notification"};

    @Test
    void modulesDoNotReachIntoEachOthersInternalPackages() {
        for (String module : MODULES) {
            ArchRule rule = noClasses()
                    .that().resideOutsideOfPackage(BASE_PACKAGE + "." + module + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            BASE_PACKAGE + "." + module + ".domain..",
                            BASE_PACKAGE + "." + module + ".adapter..")
                    .because(module + " 모듈은 application 패키지만이 다른 모듈이 의존할 수 있는 공개 API 이다");
            rule.check(CLASSES);
        }
    }

    @Test
    void domainPackagesStayFrameworkFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..", "com.fasterxml.jackson..")
                .because("domain 은 JPA/Spring/Jackson 등 어떤 전달/영속 기술도 알아서는 안 된다");
        rule.check(CLASSES);
    }

    @Test
    void modulesAreFreeOfCycles() {
        ArchRule rule = SlicesRuleDefinition.slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles();
        rule.check(CLASSES);
    }
}
