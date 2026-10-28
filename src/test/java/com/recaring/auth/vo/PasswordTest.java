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
    @DisplayName("최소 길이인 8자 비밀번호는 생성된다")
    void create_success_with_min_length_password() {
        Password password = new Password("abcde123");
        assertThat(password.value()).isEqualTo("abcde123");
    }

    @Test
    @DisplayName("최대 길이인 20자 비밀번호는 생성된다")
    void create_success_with_max_length_password() {
        Password password = new Password("abcdefghij1234567890");
        assertThat(password.value()).isEqualTo("abcdefghij1234567890");
    }

    @Test
    @DisplayName("비밀번호가 null이면 PASSWORD_IS_NULL 예외가 발생한다")
    void create_fail_when_password_is_null() {
        assertThatThrownBy(() -> new Password(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PASSWORD_IS_NULL);
    }

    @Test
    @DisplayName("비밀번호가 공백뿐이면 PASSWORD_IS_NULL 예외가 발생한다")
    void create_fail_when_password_is_blank() {
        assertThatThrownBy(() -> new Password("   "))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PASSWORD_IS_NULL);
    }

    @Test
    @DisplayName("8자 미만이면 INVALID_PASSWORD_LENGTH 예외가 발생한다")
    void create_fail_when_password_too_short() {
        assertThatThrownBy(() -> new Password("abc1234"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("20자를 넘으면 INVALID_PASSWORD_LENGTH 예외가 발생한다")
    void create_fail_when_password_too_long() {
        assertThatThrownBy(() -> new Password("abcdefghij12345678901"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("숫자가 없으면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_no_number() {
        assertThatThrownBy(() -> new Password("passwordonly"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("영문이 없으면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_no_letter() {
        assertThatThrownBy(() -> new Password("12345678"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("영문·숫자에 특수문자가 섞인 비밀번호는 생성된다")
    void create_success_with_special_character() {
        Password password = new Password("p@ssw0rd#1");
        assertThat(password.value()).isEqualTo("p@ssw0rd#1");
    }

    @Test
    @DisplayName("허용 범위 양 끝 특수문자 '!'와 '~'를 포함한 비밀번호는 생성된다")
    void create_success_with_boundary_special_characters() {
        Password password = new Password("!abcd123~");
        assertThat(password.value()).isEqualTo("!abcd123~");
    }

    @Test
    @DisplayName("공백이 섞이면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_space() {
        assertThatThrownBy(() -> new Password("abcd 1234"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("허용 범위 바로 위 제어문자(DEL)가 섞이면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_del_character() {
        assertThatThrownBy(() -> new Password("abcd1234\u007F"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("한글이 섞이면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_korean() {
        assertThatThrownBy(() -> new Password("비밀번호abc123"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("특수문자와 숫자만 있고 영문이 없으면 INVALID_PASSWORD_FORMAT 예외가 발생한다")
    void create_fail_when_password_has_special_and_number_only() {
        assertThatThrownBy(() -> new Password("!@#$1234"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
    }
}
