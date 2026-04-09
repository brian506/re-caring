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

    @InjectMocks
    private TokenIssuer tokenIssuer;

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private RefreshTokenWriter refreshTokenWriter;

    @Test
    @DisplayName("토큰 발급 성공 - JWT 생성 및 리프레시 토큰 저장")
    void issue_success() {
        // given
        Member member = MemberFixture.createMember();
        Jwt expectedJwt = new Jwt("access-token-123", "refresh-token-456");

        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(expectedJwt);

        // when
        Jwt result = tokenIssuer.issue(member);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token-123");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-456");
        then(jwtGenerator).should(times(1)).generateJwt(any(TokenPayload.class));
        then(refreshTokenWriter).should(times(1)).save("refresh-token-456", member.getMemberKey());
    }

    @Test
    @DisplayName("토큰 발급 - TokenPayload에 올바른 멤버 정보 전달")
    void issue_verify_token_payload() {
        // given
        Member member = MemberFixture.createMember();
        Jwt expectedJwt = new Jwt("access-123", "refresh-456");

        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(expectedJwt);

        // when
        tokenIssuer.issue(member);

        // then
        ArgumentCaptor<TokenPayload> payloadCaptor = ArgumentCaptor.forClass(TokenPayload.class);
        then(jwtGenerator).should(times(1)).generateJwt(payloadCaptor.capture());

        TokenPayload capturedPayload = payloadCaptor.getValue();
        assertThat(capturedPayload.memberKey()).isEqualTo(member.getMemberKey());
        assertThat(capturedPayload.role()).isEqualTo(member.getRole());
        assertThat(capturedPayload.date()).isNotNull();
    }

    @Test
    @DisplayName("토큰 발급 - 리프레시 토큰이 Redis에 저장됨")
    void issue_verify_refresh_token_saved() {
        // given
        Member member = MemberFixture.createMember();
        String refreshToken = "refresh-token-stored";
        Jwt expectedJwt = new Jwt("access-token", refreshToken);

        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(expectedJwt);

        // when
        Jwt result = tokenIssuer.issue(member);

        // then
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
        then(refreshTokenWriter).should(times(1)).save(refreshToken, member.getMemberKey());
    }

    @Test
    @DisplayName("토큰 발급 - 다양한 역할의 회원")
    void issue_with_different_member_roles() {
        // given
        Member guardianMember = MemberFixture.createMember();
        Jwt guardianJwt = new Jwt("guardian-access", "guardian-refresh");

        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(guardianJwt);

        // when
        Jwt result = tokenIssuer.issue(guardianMember);

        // then
        assertThat(result).isNotNull();
        then(jwtGenerator).should(times(1)).generateJwt(any(TokenPayload.class));
        then(refreshTokenWriter).should(times(1))
            .save("guardian-refresh", guardianMember.getMemberKey());
    }

    @Test
    @DisplayName("토큰 발급 - 각 멤버마다 고유한 리프레시 토큰")
    void issue_unique_refresh_token_per_member() {
        // given
        Member member1 = MemberFixture.createMember("01011111111");
        Member member2 = MemberFixture.createMember("01022222222");

        Jwt jwt1 = new Jwt("access-1", "refresh-1");
        Jwt jwt2 = new Jwt("access-2", "refresh-2");

        given(jwtGenerator.generateJwt(any(TokenPayload.class)))
            .willReturn(jwt1)
            .willReturn(jwt2);

        // when
        Jwt result1 = tokenIssuer.issue(member1);
        Jwt result2 = tokenIssuer.issue(member2);

        // then
        assertThat(result1.refreshToken()).isNotEqualTo(result2.refreshToken());
        then(refreshTokenWriter).should(times(1)).save("refresh-1", member1.getMemberKey());
        then(refreshTokenWriter).should(times(1)).save("refresh-2", member2.getMemberKey());
    }

    @Test
    @DisplayName("토큰 발급 - JwtGenerator 호출 시 현재 시간 포함")
    void issue_token_payload_contains_current_date() {
        // given
        Member member = MemberFixture.createMember();
        Jwt expectedJwt = new Jwt("access", "refresh");
        long beforeIssue = System.currentTimeMillis();

        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(expectedJwt);

        // when
        tokenIssuer.issue(member);
        long afterIssue = System.currentTimeMillis();

        // then
        ArgumentCaptor<TokenPayload> payloadCaptor = ArgumentCaptor.forClass(TokenPayload.class);
        then(jwtGenerator).should(times(1)).generateJwt(payloadCaptor.capture());

        TokenPayload capturedPayload = payloadCaptor.getValue();
        long payloadTime = capturedPayload.date().getTime();

        assertThat(payloadTime).isGreaterThanOrEqualTo(beforeIssue);
        assertThat(payloadTime).isLessThanOrEqualTo(afterIssue);
    }
}
