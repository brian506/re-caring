package com.recaring.device;

import com.recaring.device.dataaccess.entity.WardDeviceToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WardDeviceToken 단위 테스트")
class WardDeviceTokenTest {

    @Test
    @DisplayName("신규 생성 시 UUID 기반 토큰이 자동으로 할당된다")
    void build_assigns_token() {
        WardDeviceToken token = WardDeviceToken.builder()
                .wardKey("ward-key-001")
                .build();

        assertThat(token.getToken()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("reissue() 호출 시 기존 토큰과 다른 새 토큰이 발급된다")
    void reissue_changes_token() {
        WardDeviceToken token = WardDeviceToken.builder()
                .wardKey("ward-key-001")
                .build();
        String original = token.getToken();

        token.reissue();

        assertThat(token.getToken()).isNotEqualTo(original);
    }
}
