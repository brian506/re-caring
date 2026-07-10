package com.recaring.location.controller;

import com.recaring.support.exception.AppException;
import com.recaring.support.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// LocationController#streamLocation()은 produces=text/event-stream으로 선언돼 있어, 요청의
// Accept 헤더도 text/event-stream으로 고정된다(OkHttp SSE 클라이언트). validateCaregiverAccess()가
// SseEmitter 생성 전에 던지는 AppException을 전역 ApiControllerAdvice가 처리하면, 콘텐츠 협상이
// text/event-stream만으로 좁혀져 JSON 컨버터를 못 찾고 500으로 깨진다. assignableTypes로 이
// 컨트롤러가 던진 AppException만 가로채 Content-Type을 명시적으로 고정해 협상 자체를 건너뛴다.
// (ApiControllerAdvice는 원본 그대로, 다른 컨트롤러/엔드포인트는 영향 없음)
@Slf4j
@RestControllerAdvice(assignableTypes = LocationController.class)
public class LocationControllerAdvice {

    @NullMarked
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<@Nullable Object>> handleAppException(AppException e) {
        log.warn("[AppException]: status={} | errorCode={} | message={}",
                e.getErrorType().getStatus().value(), e.getErrorType().getErrorCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorType().getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getErrorType(), e.getData()));
    }
}
