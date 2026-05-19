package com.recaring.notification.implement;

import com.recaring.notification.business.command.UpsertFcmDeviceTokenCommand;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.NotificationRecipientType;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        UpsertFcmDeviceTokenCommand command =
                NotificationFixture.guardianTokenCommand(NotificationFixture.GUARDIAN_FCM_TOKEN);
        given(fcmDeviceTokenRepository.findByToken(NotificationFixture.GUARDIAN_FCM_TOKEN))
                .willReturn(Optional.empty());
        given(fcmDeviceTokenRepository.save(org.mockito.ArgumentMatchers.any(FcmDeviceToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        FcmDeviceToken result = fcmDeviceTokenManager.upsert(command);

        assertThat(result.getMemberKey()).isEqualTo(NotificationFixture.GUARDIAN_KEY);
        assertThat(result.getRecipientType()).isEqualTo(NotificationRecipientType.GUARDIAN);
        assertThat(result.getPlatform()).isEqualTo(FcmDevicePlatform.ANDROID);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("기존 토큰을 재할당하고 활성화한다")
    void upsert_reassigns_existing_token() {
        FcmDeviceToken existing = NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        existing.deactivate("OLD");
        UpsertFcmDeviceTokenCommand command = new UpsertFcmDeviceTokenCommand(
                NotificationFixture.MANAGER_KEY,
                NotificationRecipientType.MANAGER,
                NotificationFixture.GUARDIAN_FCM_TOKEN,
                FcmDevicePlatform.IOS
        );
        given(fcmDeviceTokenRepository.findByToken(NotificationFixture.GUARDIAN_FCM_TOKEN))
                .willReturn(Optional.of(existing));

        FcmDeviceToken result = fcmDeviceTokenManager.upsert(command);

        assertThat(result.getMemberKey()).isEqualTo(NotificationFixture.MANAGER_KEY);
        assertThat(result.getRecipientType()).isEqualTo(NotificationRecipientType.MANAGER);
        assertThat(result.getPlatform()).isEqualTo(FcmDevicePlatform.IOS);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getDeactivatedAt()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 FCM 토큰을 비활성화한다")
    void deactivateInvalidTokens_deactivates_tokens() {
        FcmDeviceToken token = NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        given(fcmDeviceTokenRepository.findByToken(NotificationFixture.GUARDIAN_FCM_TOKEN))
                .willReturn(Optional.of(token));

        fcmDeviceTokenManager.deactivateInvalidTokens(List.of(NotificationFixture.GUARDIAN_FCM_TOKEN));

        assertThat(token.isActive()).isFalse();
        assertThat(token.getDeactivationReason()).isEqualTo("FCM_UNREGISTERED");
    }

    @Test
    @DisplayName("빈 FCM 토큰을 거부한다")
    void upsert_rejects_blank_token() {
        UpsertFcmDeviceTokenCommand command = NotificationFixture.guardianTokenCommand(" ");

        assertThatThrownBy(() -> fcmDeviceTokenManager.upsert(command))
                .isInstanceOf(AppException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_FCM_DEVICE_TOKEN);

        then(fcmDeviceTokenRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("발송에 사용한 FCM 토큰의 마지막 사용 시각을 갱신한다")
    void touchLastUsedAt_updates_tokens() {
        FcmDeviceToken token = NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN);
        given(fcmDeviceTokenRepository.findAllByTokenIn(List.of(NotificationFixture.GUARDIAN_FCM_TOKEN)))
                .willReturn(List.of(token));

        fcmDeviceTokenManager.touchLastUsedAt(List.of(NotificationFixture.GUARDIAN_FCM_TOKEN));

        assertThat(token.getLastUsedAt()).isNotNull();
    }
}
