package com.recaring.config.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogDomain 로거명 → domain 판정")
class LogDomainTest {

    @ParameterizedTest
    @CsvSource({
            "com.recaring.location.implement.detection.DetectionListener, location",
            "com.recaring.common.filter.RequestIdFilter, common",
            "com.recaring.config.infra.AsyncConfig, config",
            "com.recaring.security.jwt.JwtAuthenticationFilter, security",
            "com.recaring.support.exception.GlobalExceptionHandler, support",
            "io.lettuce.core.RedisClient, lettuce",
            "org.hibernate.SQL, hibernate",
            "org.springframework.web.servlet.DispatcherServlet, spring",
            "com.zaxxer.hikari.HikariDataSource, zaxxer"
    })
    @DisplayName("앱 로거는 최상위 도메인 패키지, 외부 로거는 라이브러리명을 반환한다")
    void resolve_domain(String loggerName, String expected) {
        assertThat(LogDomain.resolve(loggerName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ROOT", "com.recaring.RecaringApplication", "com.recaring.", "org.SomeClass"})
    @DisplayName("도메인 세그먼트를 특정할 수 없으면 null을 반환한다")
    void resolve_returns_null_when_unresolvable(String loggerName) {
        assertThat(LogDomain.resolve(loggerName)).isNull();
    }

    @Test
    @DisplayName("예외를 던진 첫 앱 프레임의 도메인을 반환한다")
    void resolveOrigin_returns_domain_of_first_app_frame() {
        StackTraceElement[] stackTrace = {
                frame("com.recaring.care.implement.CareRelationshipValidator"),
                frame("com.recaring.safezone.business.SafeZoneService")
        };

        assertThat(LogDomain.resolveOrigin(stackTrace)).isEqualTo("care");
    }

    @Test
    @DisplayName("외부 라이브러리와 공용 패키지 프레임은 건너뛰고 다음 앱 프레임의 도메인을 반환한다")
    void resolveOrigin_skips_library_and_shared_frames() {
        StackTraceElement[] stackTrace = {
                frame("java.util.Optional"),
                frame("com.recaring.support.exception.AppException"),
                frame("com.recaring.common.utils.MaskingUtils"),
                frame("com.recaring.config.infra.AsyncConfig"),
                frame("com.recaring.safezone.implement.SafeZoneReader")
        };

        assertThat(LogDomain.resolveOrigin(stackTrace)).isEqualTo("safezone");
    }

    private static StackTraceElement frame(String className) {
        return new StackTraceElement(className, "method", null, 1);
    }
}
