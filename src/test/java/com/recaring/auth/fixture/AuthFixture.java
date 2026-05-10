package com.recaring.auth.fixture;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.security.vo.Jwt;

import java.time.LocalDate;

public class AuthFixture {

    public static final String EMAIL = "test@example.com";
    public static final String RAW_PASSWORD = "password1";
    public static final String ENCODED_PASSWORD = "$2a$10$encoded";
    public static final String ACCESS_TOKEN = "access-token";
    public static final String REFRESH_TOKEN = "refresh-token";
    public static final String MEMBER_KEY = "test-member-key-uuid";
    private static final long REFRESH_EXPIRATION_MS = 1209600000L; // 14 days

    public static RefreshToken createRefreshToken() {
        return RefreshToken.of(MEMBER_KEY, REFRESH_TOKEN, REFRESH_EXPIRATION_MS);
    }

    public static LocalEmail createLocalEmail() {
        return new LocalEmail(EMAIL);
    }

    public static Password createPassword() {
        return new Password(RAW_PASSWORD);
    }

    public static EncodedPassword createEncodedPassword() {
        return new EncodedPassword(ENCODED_PASSWORD);
    }

    public static SignUpCommand createSignUpCommand(String verificationToken) {
        return new SignUpCommand(
                verificationToken,
                createLocalEmail(),
                createPassword(),
                "홍길동",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                MemberRole.GUARDIAN
        );
    }

    public static Jwt createJwt() {
        return new Jwt(ACCESS_TOKEN, REFRESH_TOKEN);
    }
}
