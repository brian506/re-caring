package com.recaring.sms.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PhoneNumber VO 단위 테스트")
class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "01112345678", "01612345678", "01712345678", "01912345678"})
    @DisplayName("유효한 한국 휴대폰 번호로 객체가 생성된다")
    void create_success_with_valid_phone(String phone) {
        PhoneNumber phoneNumber = new PhoneNumber(phone);
        assertThat(phoneNumber.value()).isEqualTo(phone);
    }

    @Test
    @DisplayName("010으로 시작하는 10자리 번호도 생성된다")
    void create_success_with_010_ten_digits() {
        PhoneNumber phoneNumber = new PhoneNumber("0101234567");
        assertThat(phoneNumber.value()).isEqualTo("0101234567");
    }

    @Test
    @DisplayName("null이면 INVALID_PHONE_FORMAT 예외가 발생한다")
    void create_fail_when_null() {
        assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_PHONE_FORMAT);
    }

    @Test
    @DisplayName("공백이면 INVALID_PHONE_FORMAT 예외가 발생한다")
    void create_fail_when_blank() {
        assertThatThrownBy(() -> new PhoneNumber(""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_PHONE_FORMAT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"02012345678", "0901234567", "1234567890", "010123456789", "0101234"})
    @DisplayName("유효하지 않은 형식이면 INVALID_PHONE_FORMAT 예외가 발생한다")
    void create_fail_with_invalid_format(String phone) {
        assertThatThrownBy(() -> new PhoneNumber(phone))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_PHONE_FORMAT);
    }
}
