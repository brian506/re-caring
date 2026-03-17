package com.recaring.sms.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SmsCode VO 단위 테스트")
class SmsCodeTest {

    @Test
    @DisplayName("6자리 숫자 코드로 객체가 생성된다")
    void create_success_with_valid_code() {
        SmsCode code = new SmsCode("123456");
        assertThat(code.value()).isEqualTo("123456");
    }

    @Test
    @DisplayName("000000도 유효한 코드이다")
    void create_success_with_zero_code() {
        SmsCode code = new SmsCode("000000");
        assertThat(code.value()).isEqualTo("000000");
    }

    @Test
    @DisplayName("null이면 INVALID_VERIFICATION_CODE 예외가 발생한다")
    void create_fail_when_null() {
        assertThatThrownBy(() -> new SmsCode(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_VERIFICATION_CODE);
    }

    @Test
    @DisplayName("공백이면 INVALID_VERIFICATION_CODE 예외가 발생한다")
    void create_fail_when_blank() {
        assertThatThrownBy(() -> new SmsCode(""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_VERIFICATION_CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "abcdef", "12345a"})
    @DisplayName("6자리 숫자가 아니면 INVALID_VERIFICATION_CODE 예외가 발생한다")
    void create_fail_with_invalid_format(String code) {
        assertThatThrownBy(() -> new SmsCode(code))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_VERIFICATION_CODE);
    }

    @Test
    @DisplayName("동일한 코드는 matches()가 true를 반환한다")
    void matches_returns_true_when_same_code() {
        SmsCode code1 = new SmsCode("123456");
        SmsCode code2 = new SmsCode("123456");
        assertThat(code1.matches(code2)).isTrue();
    }

    @Test
    @DisplayName("다른 코드는 matches()가 false를 반환한다")
    void matches_returns_false_when_different_code() {
        SmsCode code1 = new SmsCode("123456");
        SmsCode code2 = new SmsCode("654321");
        assertThat(code1.matches(code2)).isFalse();
    }
}
