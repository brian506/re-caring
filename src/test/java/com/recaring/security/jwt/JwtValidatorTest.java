package com.recaring.security.jwt;

import com.recaring.security.fixture.SecurityFixture;
import com.recaring.security.vo.Jwt;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtValidator 단위 테스트")
class JwtValidatorTest {

    private JwtGenerator jwtGenerator;
    private JwtValidator jwtValidator;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = SecurityFixture.createSecretKey();
        jwtGenerator = SecurityFixture.createJwtGenerator();
        jwtValidator = SecurityFixture.createJwtValidator();
    }

    @Test
    @DisplayName("유효한 토큰은 정상적으로 파싱된다")
    void validate_success_with_valid_token() {
        String memberKey = "custom-member-key";
        Jwt jwt = jwtGenerator.generateJwt(SecurityFixture.createTokenPayload(memberKey));

        var claims = jwtValidator.validate(jwt.accessToken());

        assertThat(claims.getPayload().getSubject()).isEqualTo(memberKey);
    }

    @Test
    @DisplayName("손상된 토큰이면 MALFORMED_JWT 예외가 발생한다")
    void validate_fail_with_malformed_token() {
        assertThatThrownBy(() -> jwtValidator.validate("this.is.notvalid"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.MALFORMED_JWT);
    }

    @Test
    @DisplayName("만료된 토큰이면 EXPIRED_JWT 예외가 발생한다")
    void validate_fail_with_expired_token() {
        String expiredToken = Jwts.builder()
                .subject("test-member-key")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(secretKey)
                .compact();

        assertThatThrownBy(() -> jwtValidator.validate(expiredToken))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_JWT);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰이면 INVALID_SIGNATURE 예외가 발생한다")
    void validate_fail_with_invalid_signature() {
        String otherRawKey = "another-secret-key-for-testing-purposes-only";
        SecretKey otherKey = Keys.hmacShaKeyFor(Base64.getEncoder().encode(otherRawKey.getBytes()));

        String tokenWithOtherKey = Jwts.builder()
                .subject("test-member-key")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(otherKey)
                .compact();

        assertThatThrownBy(() -> jwtValidator.validate(tokenWithOtherKey))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("빈 문자열 토큰이면 INVALID_JWT 예외가 발생한다")
    void validate_fail_with_empty_token() {
        assertThatThrownBy(() -> jwtValidator.validate(""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_JWT);
    }
}
