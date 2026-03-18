package com.recaring.auth.business;

import com.recaring.auth.implement.RefreshTokenReader;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.domain.member.fixture.MemberFixture;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.security.jwt.JwtValidator;
import com.recaring.security.vo.Jwt;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRefreshService 단위 테스트")
class TokenRefreshServiceTest {

    @InjectMocks
    private TokenRefreshService tokenRefreshService;

    @Mock
    private JwtValidator jwtValidator;

    @Mock
    private RefreshTokenReader refreshTokenReader;

    @Mock
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private MemberReader memberReader;

    @Mock
    private TokenIssuer tokenIssuer;

    @Test
    @DisplayName("유효한 리프레시 토큰으로 새로운 JWT가 발급된다")
    void refresh_success() {
        String oldRefreshToken = "old-refresh-token";
        String memberKey = UUID.randomUUID().toString();
        Member member = MemberFixture.createMember();
        Jwt newJwt = new Jwt("new-access-token", "new-refresh-token");

        @SuppressWarnings("unchecked")
        Jws<Claims> mockClaims = mock(Jws.class);
        given(jwtValidator.validate(oldRefreshToken)).willReturn(mockClaims);
        given(refreshTokenReader.findMemberKey(oldRefreshToken)).willReturn(memberKey);
        given(memberReader.findByMemberKey(memberKey)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(newJwt);

        Jwt result = tokenRefreshService.refresh(oldRefreshToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        then(refreshTokenWriter).should(times(1)).delete(oldRefreshToken);
        then(tokenIssuer).should(times(1)).issue(member);
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 EXPIRED_JWT 예외가 발생한다")
    void refresh_fail_when_token_expired() {
        String expiredToken = "expired-refresh-token";
        willThrow(new AppException(ErrorType.EXPIRED_JWT))
                .given(jwtValidator).validate(expiredToken);

        assertThatThrownBy(() -> tokenRefreshService.refresh(expiredToken))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_JWT);
    }

    @Test
    @DisplayName("Redis에 없는 리프레시 토큰이면 EXPIRED_JWT 예외가 발생한다")
    void refresh_fail_when_token_not_in_redis() {
        String refreshToken = "not-in-redis-token";
        @SuppressWarnings("unchecked")
        Jws<Claims> mockClaims = mock(Jws.class);
        given(jwtValidator.validate(refreshToken)).willReturn(mockClaims);
        given(refreshTokenReader.findMemberKey(refreshToken))
                .willThrow(new AppException(ErrorType.EXPIRED_JWT));

        assertThatThrownBy(() -> tokenRefreshService.refresh(refreshToken))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_JWT);
    }
}
