package com.recaring.auth.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EncodedPassword VO 단위 테스트")
class EncodedPasswordTest {

    @Test
    @DisplayName("유효한 인코딩된 비밀번호로 객체가 생성된다")
    void create_success_with_valid_encoded_password() {
        String bcryptHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        EncodedPassword encodedPassword = new EncodedPassword(bcryptHash);
        assertThat(encodedPassword.value()).isEqualTo(bcryptHash);
    }

    @Test
    @DisplayName("null이면 PASSWORD_IS_NULL 예외가 발생한다")
    void create_fail_when_value_is_null() {
        assertThatThrownBy(() -> new EncodedPassword(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.PASSWORD_IS_NULL);
    }

    @Test
    @DisplayName("공백이면 PASSWORD_IS_NULL 예외가 발생한다")
    void create_fail_when_value_is_blank() {
        assertThatThrownBy(() -> new EncodedPassword("   "))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.PASSWORD_IS_NULL);
    }
}
