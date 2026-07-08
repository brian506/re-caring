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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("[요청 검증 : 실패]: message={}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ErrorType.INVALID_ACCESS_PATH, null), HttpStatus.BAD_REQUEST);
    }

    // SSE(text/event-stream) 스트림에서 클라이언트가 연결을 끊으면 emitter.send()가 실패하고,
    // Spring async 인프라가 이 실패를 error dispatch로 advice까지 올려보낸다.
    // 이 경우 응답은 이미 text/event-stream으로 확정돼 ApiResponse(JSON)를 쓸 수 없다.
    // 정상적인 클라이언트 이탈이므로 바디 없이 debug 로그만 남기고 조용히 종료한다.
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        log.debug("[SSE 이벤트 : 연결 종료]: message={}", e.getMessage());
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
        // 미처리 예외는 예외 타입과 전체 스택트레이스를 함께 남겨야 근본 원인을 진단할 수 있다.
        // (과거: stackTrace[0]만 파싱해 CGLIB 프록시 프레임·null 메시지만 찍혀 원인 불명이었음)
        log.error("[미처리 예외 : 시스템 오류]: type={} | message={}",
                e.getClass().getName(), e.getMessage(), e);
    }
}
