package com.recaring.security.fixture;

import com.recaring.domain.member.dataaccess.entity.MemberRole;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.jwt.JwtValidator;
import com.recaring.security.vo.TokenPayload;
import io.jsonwebtoken.security.Keys;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

public class SecurityFixture {

    public static final String RAW_SECRET_KEY = "recaring-test-secret-key-for-jwt-testing-only";
    public static final String MEMBER_KEY = "test-member-key-fixture";
    public static final MemberRole MEMBER_ROLE = MemberRole.GUARDIAN;
    public static final String ISSUER = "recaring";
    public static final long ACCESS_EXPIRATION = 3600000L;
    public static final long REFRESH_EXPIRATION = 1209600000L;

    /** 테스트용 SecretKey 생성 (프로덕션 JwtConfig.key()와 동일한 방식) */
    public static SecretKey createSecretKey() {
        return Keys.hmacShaKeyFor(Base64.getEncoder().encode(RAW_SECRET_KEY.getBytes()));
    }

    /**
     * @Value 필드가 주입된 JwtGenerator 생성.
     * Spring 컨텍스트 없이 단위 테스트에서 사용한다.
     */
    public static JwtGenerator createJwtGenerator() {
        JwtGenerator jwtGenerator = new JwtGenerator(createSecretKey());
        ReflectionTestUtils.setField(jwtGenerator, "issuer", ISSUER);
        ReflectionTestUtils.setField(jwtGenerator, "accessKeyExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtGenerator, "refreshKeyExpiration", REFRESH_EXPIRATION);
        return jwtGenerator;
    }

    /** JwtValidator 생성 */
    public static JwtValidator createJwtValidator() {
        return new JwtValidator(createSecretKey());
    }

    /** 기본 memberKey로 TokenPayload 생성 */
    public static TokenPayload createTokenPayload() {
        return new TokenPayload(MEMBER_KEY, MEMBER_ROLE, new Date());
    }

    /** memberKey를 지정하여 TokenPayload 생성 */
    public static TokenPayload createTokenPayload(String memberKey) {
        return new TokenPayload(memberKey, MEMBER_ROLE, new Date());
    }
}
