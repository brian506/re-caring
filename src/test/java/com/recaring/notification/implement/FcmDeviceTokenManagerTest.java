package com.recaring.notification.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FCM 디바이스 토큰 매니저 단위 테스트")
class FcmDeviceTokenManagerTest {

    @InjectMocks
    private FcmDeviceTokenManager fcmDeviceTokenManager;

    @Mock
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Test
    @DisplayName("새 FCM 디바이스 토큰을 등록한다")
    void upsert_saves_new_token_when_absent() {
        given(fcmDeviceTokenRepository.findByToken(NotificationFixture.GUARDIAN_FCM_TOKEN))
                .willReturn(Optional.empty());
        given(fcmDeviceTokenRepository.save(any(FcmDeviceToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        FcmDeviceToken result = fcmDeviceTokenManager.upsert(
                NotificationFixture.GUARDIAN_KEY,
                NotificationFixture.GUARDIAN_FCM_TOKEN,
                CareRole.GUARDIAN,
                FcmDevicePlatform.ANDROID
        );

        assertThat(result.getMemberKey()).isEqualTo(NotificationFixture.GUARDIAN_KEY);
        assertThat(result.getCareRole()).isEqualTo(CareRole.GUARDIAN);
        assertThat(result.getPlatform()).isEqualTo(FcmDevicePlatform.ANDROID);
    }

    @Test
    @DisplayName("기존 토큰을 새 소유자로 재할당한다")
    void upsert_reassigns_existing_token() {
        FcmDeviceToken existing = NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        given(fcmDeviceTokenRepository.findByToken(NotificationFixture.GUARDIAN_FCM_TOKEN))
                .willReturn(Optional.of(existing));

        FcmDeviceToken result = fcmDeviceTokenManager.upsert(
                NotificationFixture.MANAGER_KEY,
                NotificationFixture.GUARDIAN_FCM_TOKEN,
                CareRole.MANAGER,
                FcmDevicePlatform.IOS
        );

        assertThat(result.getMemberKey()).isEqualTo(NotificationFixture.MANAGER_KEY);
        assertThat(result.getCareRole()).isEqualTo(CareRole.MANAGER);
        assertThat(result.getPlatform()).isEqualTo(FcmDevicePlatform.IOS);
        then(fcmDeviceTokenRepository).should().findByToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
    }

    @Test
    @DisplayName("유효하지 않은 FCM 토큰을 삭제한다")
    void deleteInvalidTokens_deletes_tokens() {
        List<String> invalidTokens = List.of(NotificationFixture.GUARDIAN_FCM_TOKEN);

        fcmDeviceTokenManager.deleteInvalidTokens(invalidTokens);

        then(fcmDeviceTokenRepository).should().deleteByTokenIn(invalidTokens);
    }

    @Test
    @DisplayName("삭제할 토큰이 없으면 아무 동작도 하지 않는다")
    void deleteInvalidTokens_does_nothing_when_empty() {
        fcmDeviceTokenManager.deleteInvalidTokens(List.of());

        then(fcmDeviceTokenRepository).shouldHaveNoInteractions();
    }
}
