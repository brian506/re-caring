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

    private static final long REFRESH_EXPIRATION_MS = 1209600000L;

    @InjectMocks
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenWriter, "refreshExpiration", REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("리프레시 토큰과 회원 키가 각자 제 자리에 저장된다")
    void save_puts_token_and_member_key_in_their_own_fields() {
        refreshTokenWriter.save(AuthFixture.REFRESH_TOKEN, AuthFixture.MEMBER_KEY);

        // Then
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        then(refreshTokenRepository).should(times(1)).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getMemberKey()).isEqualTo(AuthFixture.MEMBER_KEY);
        assertThat(saved.getToken()).isEqualTo(AuthFixture.REFRESH_TOKEN);
    }
}
