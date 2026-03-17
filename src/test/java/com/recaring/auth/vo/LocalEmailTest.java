package com.recaring.auth.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalEmail VO 단위 테스트")
class LocalEmailTest {

    @Test
    @DisplayName("올바른 이메일 형식이면 객체가 생성된다")
    void create_success_with_valid_email() {
        LocalEmail email = new LocalEmail("test@example.com");
        assertThat(email.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("숫자와 특수문자가 포함된 이메일도 생성된다")
    void create_success_with_complex_email() {
        LocalEmail email = new LocalEmail("user.name+tag@sub.domain.com");
        assertThat(email.email()).isEqualTo("user.name+tag@sub.domain.com");
    }

    @Test
    @DisplayName("이메일이 null이면 EMAIL_IS_NULL 예외가 발생한다")
    void create_fail_when_email_is_null() {
        assertThatThrownBy(() -> new LocalEmail(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EMAIL_IS_NULL);
    }

    @Test
    @DisplayName("이메일이 빈 문자열이면 EMAIL_IS_NULL 예외가 발생한다")
    void create_fail_when_email_is_blank() {
        assertThatThrownBy(() -> new LocalEmail("   "))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EMAIL_IS_NULL);
    }

    @Test
    @DisplayName("@ 없이는 INVALID_EMAIL_FORMAT 예외가 발생한다")
    void create_fail_when_email_has_no_at_sign() {
        assertThatThrownBy(() -> new LocalEmail("invalidemail.com"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("도메인 없이는 INVALID_EMAIL_FORMAT 예외가 발생한다")
    void create_fail_when_email_has_no_domain() {
        assertThatThrownBy(() -> new LocalEmail("user@"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_EMAIL_FORMAT);
    }
}
