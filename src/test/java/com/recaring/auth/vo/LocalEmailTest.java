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
        assertThat(email.value()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("점·플러스·서브도메인이 포함된 이메일도 생성된다")
    void create_success_with_complex_email() {
        LocalEmail email = new LocalEmail("user.name+tag@sub.domain.com");
        assertThat(email.value()).isEqualTo("user.name+tag@sub.domain.com");
    }

    @Test
    @DisplayName("이메일이 null이면 EMAIL_IS_NULL 예외가 발생한다")
    void create_fail_when_email_is_null() {
        assertThatThrownBy(() -> new LocalEmail(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.EMAIL_IS_NULL);
    }

    @Test
    @DisplayName("이메일이 공백뿐이면 EMAIL_IS_NULL 예외가 발생한다")
    void create_fail_when_email_is_blank() {
        assertThatThrownBy(() -> new LocalEmail("   "))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.EMAIL_IS_NULL);
    }

    @Test
    @DisplayName("@가 없으면 INVALID_EMAIL_FORMAT 예외가 발생한다")
    void create_fail_when_email_has_no_at_sign() {
        assertThatThrownBy(() -> new LocalEmail("invalidemail.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("@ 뒤 도메인이 없으면 INVALID_EMAIL_FORMAT 예외가 발생한다")
    void create_fail_when_email_has_no_domain() {
        assertThatThrownBy(() -> new LocalEmail("user@"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_EMAIL_FORMAT);
    }
}
