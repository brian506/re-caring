package com.recaring.common.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

import static com.recaring.common.fixture.CommonFixture.APP_EXCEPTION_DOMAIN;
import static com.recaring.common.fixture.CommonFixture.ROOT_CAUSE_MESSAGE;
import static com.recaring.common.fixture.CommonFixture.appExceptionStackTrace;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppException 로그 필드 단위 테스트")
class AppExceptionLoggerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AppExceptionLogger.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("WARN 비즈니스 예외는 에러 코드·유형·상태·도메인을 필드로 남기고 스택은 남기지 않는다")
    void warn_exception_logs_fields_without_stack_trace() {
        AppException exception = new AppException(ErrorType.DUPLICATE_RESOURCE);
        exception.setStackTrace(appExceptionStackTrace());

        AppExceptionLogger.log(exception);

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(keyValues(event)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "domain", APP_EXCEPTION_DOMAIN,
                "errorCode", "E409",
                "errorType", "DUPLICATE_RESOURCE",
                "httpStatus", 409));
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    @DisplayName("ERROR 비즈니스 예외는 원인 예외까지 추적되도록 예외를 함께 남긴다")
    void error_exception_logs_cause_chain() {
        AppException exception = new AppException(ErrorType.DEFAULT_ERROR, new IllegalStateException(ROOT_CAUSE_MESSAGE));
        exception.setStackTrace(appExceptionStackTrace());

        AppExceptionLogger.log(exception);

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(keyValues(event)).containsEntry("errorCode", "E500");
        assertThat(event.getThrowableProxy().getCause().getClassName()).isEqualTo(IllegalStateException.class.getName());
        assertThat(event.getThrowableProxy().getCause().getMessage()).isEqualTo(ROOT_CAUSE_MESSAGE);
    }

    private static Map<String, Object> keyValues(ILoggingEvent event) {
        return event.getKeyValuePairs().stream()
                .collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
    }
}
