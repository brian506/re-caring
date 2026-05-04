package com.recaring.device.business;

import com.recaring.device.implement.WardDeviceTokenManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceTokenService 단위 테스트")
class DeviceTokenServiceTest {

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    @Mock
    private WardDeviceTokenManager wardDeviceTokenManager;

    @Test
    @DisplayName("기존 토큰이 없으면 새 WardDeviceToken을 저장하고 토큰 값을 반환한다")
    void issueToken_creates_new_token_when_not_exists() {
        given(wardDeviceTokenManager.issueToken("ward-key-001")).willReturn("new-token-value");

        String token = deviceTokenService.issueToken("ward-key-001");

        assertThat(token).isEqualTo("new-token-value");
        then(wardDeviceTokenManager).should(times(1)).issueToken("ward-key-001");
    }

    @Test
    @DisplayName("기존 토큰이 있으면 reissue()를 호출하여 새 토큰을 반환한다")
    void issueToken_reissues_existing_token() {
        given(wardDeviceTokenManager.issueToken("ward-key-001")).willReturn("reissued-token-value");

        String token = deviceTokenService.issueToken("ward-key-001");

        assertThat(token).isEqualTo("reissued-token-value");
        then(wardDeviceTokenManager).should(times(1)).issueToken("ward-key-001");
    }
}
