package com.recaring.auth.implement;

import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.auth.dataaccess.repository.RefreshTokenRepository;
import com.recaring.auth.fixture.AuthFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenWriter 단위 테스트")
class RefreshTokenWriterTest {

    @InjectMocks
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenWriter, "refreshExpiration", 1209600000L);
    }

    @Test
    @DisplayName("리프레시 토큰을 DB에 저장한다")
    void save_success() {
        // when
        refreshTokenWriter.save(AuthFixture.REFRESH_TOKEN, AuthFixture.MEMBER_KEY);

        // then
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getMemberKey()).isEqualTo(AuthFixture.MEMBER_KEY);
        assertThat(captor.getValue().getToken()).isEqualTo(AuthFixture.REFRESH_TOKEN);
        assertThat(captor.getValue().isExpired()).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰을 DB에서 삭제한다")
    void delete_success() {
        // when
        refreshTokenWriter.delete(AuthFixture.REFRESH_TOKEN);

        // then
        then(refreshTokenRepository).should(times(1)).deleteByToken(AuthFixture.REFRESH_TOKEN);
    }
}
