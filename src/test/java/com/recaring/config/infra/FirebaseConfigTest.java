package com.recaring.config.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Firebase 설정 단위 테스트")
class FirebaseConfigTest {

    @Test
    @DisplayName("서비스 계정 값이 Base64 형식이 아니면 설정 오류를 명확히 던진다")
    void firebaseApp_throws_clear_exception_when_service_account_is_not_base64() {
        FirebaseConfig firebaseConfig = new FirebaseConfig();
        ReflectionTestUtils.setField(firebaseConfig, "serviceAccountJsonBase64", "not-base64!");

        assertThatThrownBy(firebaseConfig::firebaseApp)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("firebase.service-account-json-base64 must be valid Base64 encoded JSON.")
                .satisfies(exception -> assertThat(exception.getCause())
                        .isInstanceOf(IllegalArgumentException.class));
    }
}
