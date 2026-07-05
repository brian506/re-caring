package com.recaring.notification.implement;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FCM 디바이스 토큰 Reader 단위 테스트")
class FcmDeviceTokenReaderTest {

    @InjectMocks
    private FcmDeviceTokenReader fcmDeviceTokenReader;

    @Mock
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Test
    @DisplayName("findTokensByMemberKey - 회원의 모든 토큰을 조회한다")
    void findTokensByMemberKey_returns_member_tokens() {
        List<FcmDeviceToken> tokens = List.of(
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN)
        );
        given(fcmDeviceTokenRepository.findByMemberKey(NotificationFixture.GUARDIAN_KEY)).willReturn(tokens);

        List<FcmDeviceToken> result = fcmDeviceTokenReader.findTokensByMemberKey(NotificationFixture.GUARDIAN_KEY);

        assertThat(result).isEqualTo(tokens);
        then(fcmDeviceTokenRepository).should().findByMemberKey(NotificationFixture.GUARDIAN_KEY);
    }
}
