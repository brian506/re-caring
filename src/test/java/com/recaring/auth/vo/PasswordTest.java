package com.recaring.auth.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Password VO 단위 테스트")
class PasswordTest {

    @Test
    @DisplayName("영문과 숫자를 포함한 8자 이상 20자 이하 비밀번호는 생성된다")
    void create_success_with_valid_password() {
        Password password = new Password("password1");
        assertThat(password.password()).isEqualTo("password1");
    }

    @Test
    @DisplayName("최소 길이(8자) 비밀번호는 생성된다")
    void create_success_with_min_length_password() {
        Password password = new Password("abcde123");
        assertThat(password.password()).isEqualTo("abcde123");
    }

    @Test
    @DisplayName("최대 길이(20자) 비밀번호는 생성된다")
    void create_success_with_max_length_password() {
        Password password = new Password("abcdefghij1234567890");
        assertThat(password.password()).isEqualTo("abcdefghij1234567890");
    }

    @Test
    @DisplayName("비밀번호가 null이면 PASSWORD_IS_NULL 예외가 발생한다")
    void create_fail_when_password_is_null() {
        assertThatThrownBy(() -> new Password(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.PASSWORD_IS_NULL);
    }

    @Test
    @DisplayName("비밀번호가 공백이면 PASSWORD_IS_NULL 예외가 발생한다")
    void create_fail_when_password_is_blank() {
        assertThatThrownBy(() -> new Password("   "))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.PASSWORD_IS_NULL);
    }

    @Test
    @DisplayName("8자 미만이면 INVALID_PASSWORD_LENGTH 예외가 발생한다")
    void create_fail_when_password_too_short() {
        assertThatThrownBy(() -> new Password("abc1234"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("20자 초과이면 INVALID_PASSWORD_LENGTH 예외가 발생한다")
    void create_fail_when_password_too_long() {
        assertThatThrownBy(() -> new Password("abcdefghij12345678901"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("숫자가 없으면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_no_number() {
        assertThatThrownBy(() -> new Password("passwordonly"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("영문이 없으면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_no_letter() {
        assertThatThrownBy(() -> new Password("12345678"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT);
    }
}
