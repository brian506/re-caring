package com.recaring.common.controller;


import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorCode;
import com.recaring.support.exception.ErrorType;
import com.recaring.support.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiControllerAdvice {

    @NullMarked
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleAppException(AppException e) {
        printAppExceptionLog(e);

        return new ResponseEntity<>(ApiResponse.error(e.getErrorType(), e.getData()), e.getErrorType().getStatus());
    }

    @NullMarked
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleOptimisticLockException(ObjectOptimisticLockingFailureException e) {
        log.warn("[OptimisticLockException]: message={}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ErrorType.NOTIFICATION_SETTING_UPDATE_CONFLICT, null), HttpStatus.CONFLICT);
    }

    @NullMarked
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleException(Exception e) {
        printExceptionLog(e);
        return new ResponseEntity<>(ApiResponse.error(ErrorType.DEFAULT_ERROR, null), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void printAppExceptionLog(AppException e) {
        String className = e.getStackTrace()[0].getClassName();
        String methodName = e.getStackTrace()[0].getMethodName();
        int lineNumber = e.getStackTrace()[0].getLineNumber();
        int status = e.getErrorType().getStatus().value();
        ErrorCode errorCode = e.getErrorType().getErrorCode();
        String message = e.getMessage();
        Object data = e.getData();

        switch (e.getErrorType().getLogLevel()) {
            case ERROR ->
                    log.error("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                            className, methodName, lineNumber, status, errorCode, message, data);
            case WARN ->
                    log.warn("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                            className, methodName, lineNumber, status, errorCode, message, data);
            default ->
                    log.info("[AppException]: class={} | method={} | line={} | status={} | errorCode={} | message={} | data={}",
                            className, methodName, lineNumber, status, errorCode, message, data);
        }
    }

    private void printExceptionLog(Exception e) {
        String className = e.getStackTrace()[0].getClassName();
        String methodName = e.getStackTrace()[0].getMethodName();
        int lineNumber = e.getStackTrace()[0].getLineNumber();
        String message = e.getMessage();

        log.error("[AppException]: class={} | method={} | line={} | message={}",
                className, methodName, lineNumber, message);
    }
}
