package com.recaring.notification.business;

import com.recaring.care.dataaccess.entity.CarePartyRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.fcm.FcmDeviceTokenManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmDeviceTokenService 단위 테스트")
class FcmDeviceTokenServiceTest {

    @InjectMocks
    private FcmDeviceTokenService fcmDeviceTokenService;

    @Mock
    private FcmDeviceTokenManager fcmDeviceTokenManager;

    @Test
    @DisplayName("FCM 토큰 등록/수정을 FcmDeviceTokenManager에 위임한다")
    void upsert_delegates_to_manager() {
        FcmDeviceToken saved = NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        given(fcmDeviceTokenManager.upsert(
                NotificationFixture.GUARDIAN_KEY,
                NotificationFixture.GUARDIAN_FCM_TOKEN,
                CarePartyRole.GUARDIAN,
                FcmDevicePlatform.ANDROID))
                .willReturn(saved);

        FcmDeviceToken result = fcmDeviceTokenService.upsert(
                NotificationFixture.GUARDIAN_KEY,
                NotificationFixture.GUARDIAN_FCM_TOKEN,
                CarePartyRole.GUARDIAN,
                FcmDevicePlatform.ANDROID);

        assertThat(result).isEqualTo(saved);
        then(fcmDeviceTokenManager).should(times(1)).upsert(
                NotificationFixture.GUARDIAN_KEY,
                NotificationFixture.GUARDIAN_FCM_TOKEN,
                CarePartyRole.GUARDIAN,
                FcmDevicePlatform.ANDROID);
    }

    @Test
    @DisplayName("FCM 토큰 삭제를 FcmDeviceTokenManager에 위임한다")
    void delete_delegates_to_manager() {
        fcmDeviceTokenService.delete(NotificationFixture.GUARDIAN_FCM_TOKEN);

        then(fcmDeviceTokenManager).should(times(1)).deleteByToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
    }
}
