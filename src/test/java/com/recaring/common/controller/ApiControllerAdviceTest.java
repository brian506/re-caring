package com.recaring.common.controller;

import com.recaring.support.exception.ErrorType;
import com.recaring.support.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiControllerAdvice 단위 테스트")
class ApiControllerAdviceTest {

    private final ApiControllerAdvice apiControllerAdvice = new ApiControllerAdvice();

    @Test
    @DisplayName("필수 요청 파라미터가 누락되면 400과 INVALID_ACCESS_PATH를 반환한다")
    void handleRequestParamException_returns_400_when_parameter_missing() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("wardKey", "String");

        ResponseEntity<ApiResponse<Object>> response = apiControllerAdvice.handleRequestParamException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().errorCode()).isEqualTo(ErrorType.INVALID_ACCESS_PATH.getErrorCode().name());
    }

    @Test
    @DisplayName("요청 파라미터 타입 변환에 실패하면 400과 INVALID_ACCESS_PATH를 반환한다")
    void handleRequestParamException_returns_400_when_type_mismatch() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "not-a-number", Integer.class, "page", null, new IllegalArgumentException("not-a-number"));

        ResponseEntity<ApiResponse<Object>> response = apiControllerAdvice.handleRequestParamException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().errorCode()).isEqualTo(ErrorType.INVALID_ACCESS_PATH.getErrorCode().name());
    }

    @Test
    @DisplayName("SSE 클라이언트가 연결을 끊으면 예외 없이 조용히 종료한다")
    void handleAsyncRequestNotUsable_does_not_throw() {
        AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException("client disconnected");

        assertThatCode(() -> apiControllerAdvice.handleAsyncRequestNotUsable(exception))
                .doesNotThrowAnyException();
    }
}
