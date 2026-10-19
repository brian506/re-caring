package com.recaring.config.logging;

import org.junit.jupiter.api.DisplayName;
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
}
