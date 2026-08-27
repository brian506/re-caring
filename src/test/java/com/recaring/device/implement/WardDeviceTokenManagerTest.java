package com.recaring.device.implement;

import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.device.fixture.DeviceFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("WardDeviceTokenManager 단위 테스트")
class WardDeviceTokenManagerTest {

    @InjectMocks
    private WardDeviceTokenManager wardDeviceTokenManager;

    @Mock
    private WardDeviceTokenRepository wardDeviceTokenRepository;

    @Mock
    private WardDeviceTokenReader wardDeviceTokenReader;

    @Captor
    private ArgumentCaptor<WardDeviceToken> tokenCaptor;

    @Test
    @DisplayName("발급 이력이 없는 보호대상자는 새 행이 저장되고 그 토큰이 반환된다")
    void issueToken_saves_new_row_when_no_previous_token() {
        // given
        given(wardDeviceTokenRepository.findByWardKey(DeviceFixture.WARD_KEY)).willReturn(Optional.empty());

        // when
        String issued = wardDeviceTokenManager.issueToken(DeviceFixture.WARD_KEY);

        // then
        then(wardDeviceTokenRepository).should().save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getWardKey()).isEqualTo(DeviceFixture.WARD_KEY);
        assertThat(issued).isEqualTo(tokenCaptor.getValue().getToken());
        then(wardDeviceTokenReader).should(never()).evict(any());
    }

    @Test
    @DisplayName("재발급 시 캐시는 새 토큰이 아니라 직전 토큰 기준으로 무효화된다")
    void issueToken_evicts_cache_by_previous_token_on_reissue() {
        // given
        WardDeviceToken existing = DeviceFixture.createDeviceToken(DeviceFixture.WARD_KEY);
        String previousToken = existing.getToken();
        given(wardDeviceTokenRepository.findByWardKey(DeviceFixture.WARD_KEY)).willReturn(Optional.of(existing));

        // when
        String issued = wardDeviceTokenManager.issueToken(DeviceFixture.WARD_KEY);

        // then
        assertThat(issued).isNotEqualTo(previousToken);
        then(wardDeviceTokenReader).should().evict(previousToken);
        then(wardDeviceTokenReader).should(never()).evict(issued);
    }

    @Test
    @DisplayName("재발급은 기존 행을 갱신할 뿐 새 행을 저장하지 않는다")
    void issueToken_does_not_insert_new_row_on_reissue() {
        // given
        WardDeviceToken existing = DeviceFixture.createDeviceToken(DeviceFixture.WARD_KEY);
        given(wardDeviceTokenRepository.findByWardKey(DeviceFixture.WARD_KEY)).willReturn(Optional.of(existing));

        // when
        wardDeviceTokenManager.issueToken(DeviceFixture.WARD_KEY);

        // then
        then(wardDeviceTokenRepository).should(never()).save(any());
    }
}
