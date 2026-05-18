package com.recaring.auth.implement;

import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.auth.dataaccess.repository.RefreshTokenRepository;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenReader 단위 테스트")
class RefreshTokenReaderTest {

    @InjectMocks
    private RefreshTokenReader refreshTokenReader;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("유효한 토큰으로 memberKey 조회 성공")
    void findMemberKey_success() {
        // given
        RefreshToken entity = AuthFixture.createRefreshToken();
        given(refreshTokenRepository.findByToken(AuthFixture.REFRESH_TOKEN)).willReturn(Optional.of(entity));

        // when
        String result = refreshTokenReader.findMemberKey(AuthFixture.REFRESH_TOKEN);

        // then
        assertThat(result).isEqualTo(AuthFixture.MEMBER_KEY);
    }

    @Test
    @DisplayName("DB에 존재하지 않는 토큰이면 EXPIRED_JWT 예외가 발생한다")
    void findMemberKey_fail_when_not_found() {
        // given
        given(refreshTokenRepository.findByToken(AuthFixture.REFRESH_TOKEN)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refreshTokenReader.findMemberKey(AuthFixture.REFRESH_TOKEN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.EXPIRED_JWT);
    }

    @Test
    @DisplayName("만료된 토큰이면 DB에서 삭제 후 EXPIRED_JWT 예외가 발생한다")
    void findMemberKey_fail_when_expired() {
        // given
        RefreshToken expiredToken = AuthFixture.createRefreshToken();
        ReflectionTestUtils.setField(expiredToken, "expiredAt", LocalDateTime.now().minusDays(1));
        given(refreshTokenRepository.findByToken(AuthFixture.REFRESH_TOKEN)).willReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> refreshTokenReader.findMemberKey(AuthFixture.REFRESH_TOKEN))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.EXPIRED_JWT);
        then(refreshTokenRepository).should(times(1)).deleteByToken(AuthFixture.REFRESH_TOKEN);
    }
}
