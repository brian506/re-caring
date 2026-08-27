package com.recaring.auth.implement;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.Jwt;
import com.recaring.security.vo.TokenPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenIssuer 단위 테스트")
class TokenIssuerTest {

    private static final String ACCESS_TOKEN = "issued-access-token";
    private static final String REFRESH_TOKEN = "issued-refresh-token";
    private static final String MEMBER_KEY = "issuer-member-key-uuid";

    @InjectMocks
    private TokenIssuer tokenIssuer;

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private RefreshTokenWriter refreshTokenWriter;

    @Test
    @DisplayName("발급한 JWT 중 리프레시 토큰만 해당 회원 키로 저장된다")
    void issue_stores_only_the_refresh_token_for_that_member() {
        Member member = MemberFixture.createMemberWithKey(MEMBER_KEY, MemberFixture.PHONE);
        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(new Jwt(ACCESS_TOKEN, REFRESH_TOKEN));

        // When
        Jwt result = tokenIssuer.issue(member);

        then(refreshTokenWriter).should(times(1)).save(REFRESH_TOKEN, MEMBER_KEY);
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 페이로드에는 회원의 식별자와 역할이 담긴다")
    void issue_puts_member_key_and_role_into_payload() {
        // Given
        Member member = MemberFixture.createMemberWithKey(MEMBER_KEY, MemberFixture.PHONE);
        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(new Jwt(ACCESS_TOKEN, REFRESH_TOKEN));

        // When
        tokenIssuer.issue(member);

        // Then
        ArgumentCaptor<TokenPayload> captor = ArgumentCaptor.forClass(TokenPayload.class);
        then(jwtGenerator).should(times(1)).generateJwt(captor.capture());
        TokenPayload payload = captor.getValue();

        assertThat(payload.memberKey()).isEqualTo(MEMBER_KEY);
        assertThat(payload.role()).isEqualTo(MemberFixture.ROLE);
    }
}
