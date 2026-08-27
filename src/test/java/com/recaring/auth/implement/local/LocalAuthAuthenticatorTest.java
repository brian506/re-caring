package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthAuthenticator 단위 테스트")
class LocalAuthAuthenticatorTest {

    private static final String OWNER_MEMBER_KEY = "owner-member-key-uuid";

    @InjectMocks
    private LocalAuthAuthenticator localAuthAuthenticator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private MemberReader memberReader;

    private LocalAuth storedAuth() {
        return LocalAuth.builder()
                .memberKey(OWNER_MEMBER_KEY)
                .email(AuthFixture.EMAIL)
                .password(AuthFixture.ENCODED_PASSWORD)
                .build();
    }

    @Test
    @DisplayName("비밀번호가 일치하면 그 이메일에 매인 회원을 돌려준다")
    void authenticate_returns_member_owning_the_email() {
        // Given
        LocalEmail email = AuthFixture.createLocalEmail();
        Password password = AuthFixture.createPassword();
        Member owner = MemberFixture.createMemberWithKey(OWNER_MEMBER_KEY, MemberFixture.PHONE);

        given(localAuthReader.findByEmail(AuthFixture.EMAIL)).willReturn(storedAuth());
        given(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, AuthFixture.ENCODED_PASSWORD)).willReturn(true);
        given(memberReader.findByMemberKey(OWNER_MEMBER_KEY)).willReturn(owner);

        // When
        Member result = localAuthAuthenticator.authenticate(email, password);

        assertThat(result.getMemberKey()).isEqualTo(OWNER_MEMBER_KEY);
        assertThat(result.getPhone()).isEqualTo(MemberFixture.PHONE);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 INVALID_PASSWORD 예외가 발생하고 회원을 조회하지 않는다")
    void authenticate_throws_and_skips_member_lookup_when_password_mismatches() {
        // Given
        LocalEmail email = AuthFixture.createLocalEmail();
        Password password = AuthFixture.createPassword();

        given(localAuthReader.findByEmail(AuthFixture.EMAIL)).willReturn(storedAuth());
        given(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, AuthFixture.ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> localAuthAuthenticator.authenticate(email, password))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);

        then(memberReader).should(never()).findByMemberKey(anyString());
    }

    @Test
    @DisplayName("memberKey로 조회한 비밀번호가 일치하면 통과한다")
    void verifyPassword_passes_when_password_matches() {
        // Given
        Password password = AuthFixture.createPassword();
        given(localAuthReader.findByMemberKey(AuthFixture.MEMBER_KEY)).willReturn(storedAuth());
        given(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, AuthFixture.ENCODED_PASSWORD)).willReturn(true);

        assertThatCode(() -> localAuthAuthenticator.verifyPassword(AuthFixture.MEMBER_KEY, password))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("memberKey로 조회한 비밀번호가 일치하지 않으면 INVALID_PASSWORD 예외가 발생한다")
    void verifyPassword_throws_when_password_mismatches() {
        // Given
        Password password = AuthFixture.createPassword();
        given(localAuthReader.findByMemberKey(AuthFixture.MEMBER_KEY)).willReturn(storedAuth());
        given(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, AuthFixture.ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> localAuthAuthenticator.verifyPassword(AuthFixture.MEMBER_KEY, password))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);
    }
}
