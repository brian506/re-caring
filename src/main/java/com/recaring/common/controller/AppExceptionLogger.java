package com.recaring.common.controller;

import com.recaring.config.logging.LogDomain;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;

@Slf4j
public final class AppExceptionLogger {

    private AppExceptionLogger() {
    }

    public static void log(AppException e) {
        ErrorType errorType = e.getErrorType();
        StackTraceElement[] stackTrace = e.getStackTrace();
        StackTraceElement thrownAt = stackTrace.length > 0 ? stackTrace[0] : null;

        LoggingEventBuilder event = switch (errorType.getLogLevel()) {
            case ERROR -> log.atError().setCause(e);
            case WARN -> log.atWarn();
            default -> log.atInfo();
        };

        String domain = LogDomain.resolveOrigin(stackTrace);
        if (domain != null) {
            event = event.addKeyValue(LogDomain.FIELD, domain);
        }

        event.addKeyValue("errorCode", errorType.getErrorCode().name())
                .addKeyValue("errorType", errorType.name())
                .addKeyValue("httpStatus", errorType.getStatus().value())
                .log("[AppException : Handled]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                        thrownAt == null ? null : thrownAt.getClassName(),
                        thrownAt == null ? null : thrownAt.getMethodName(),
                        thrownAt == null ? null : thrownAt.getLineNumber(),
                        errorType.getStatus().value(),
                        errorType.getErrorCode(),
                        e.getMessage(),
                        e.getData());
    }
}
