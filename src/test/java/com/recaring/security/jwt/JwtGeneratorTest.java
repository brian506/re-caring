package com.recaring.security.jwt;

import com.recaring.domain.member.MemberRole;
import com.recaring.security.fixture.SecurityFixture;
import com.recaring.security.vo.Jwt;
import com.recaring.security.vo.TokenPayload;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtGenerator 단위 테스트")
class JwtGeneratorTest {

    private JwtGenerator jwtGenerator;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = SecurityFixture.createSecretKey();
        jwtGenerator = SecurityFixture.createJwtGenerator();
    }

    @Test
    @DisplayName("유효한 TokenPayload로 Access Token과 Refresh Token이 생성된다")
    void generateJwt_success() {
        TokenPayload payload = SecurityFixture.createTokenPayload();

        Jwt jwt = jwtGenerator.generateJwt(payload);

        assertThat(jwt.accessToken()).isNotBlank();
        assertThat(jwt.refreshToken()).isNotBlank();
        assertThat(jwt.accessToken()).isNotEqualTo(jwt.refreshToken());
    }

    @Test
    @DisplayName("생성된 Access Token에 role 클레임이 포함된다")
    void generateJwt_access_token_contains_role() {
        String memberKey = "custom-member-key";
        TokenPayload payload = SecurityFixture.createTokenPayload(memberKey);

        Jwt jwt = jwtGenerator.generateJwt(payload);

        Jws<Claims> claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(jwt.accessToken());

        assertThat(claims.getPayload().getSubject()).isEqualTo(memberKey);
        assertThat(claims.getPayload().get("role", String.class)).isEqualTo(MemberRole.GUARDIAN.name());
    }

    @Test
    @DisplayName("memberKey가 null이면 INVALID_MEMBER_KEY 예외가 발생한다")
    void generateJwt_fail_when_member_key_is_null() {
        TokenPayload payload = new TokenPayload(null, MemberRole.GUARDIAN, new Date());

        assertThatThrownBy(() -> jwtGenerator.generateJwt(payload))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_MEMBER_KEY);
    }

    @Test
    @DisplayName("memberKey가 공백이면 INVALID_MEMBER_KEY 예외가 발생한다")
    void generateJwt_fail_when_member_key_is_blank() {
        TokenPayload payload = new TokenPayload("   ", MemberRole.GUARDIAN, new Date());

        assertThatThrownBy(() -> jwtGenerator.generateJwt(payload))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_MEMBER_KEY);
    }
}
