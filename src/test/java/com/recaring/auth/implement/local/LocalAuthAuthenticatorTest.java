package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.Password;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthAuthenticator 단위 테스트")
class LocalAuthAuthenticatorTest {

    private static final String OWNER_MEMBER_KEY = "owner-member-key-uuid";
    private static final String DUMMY_ENCODED_PASSWORD = "$2a$10$dummy";

    private LocalAuthAuthenticator localAuthAuthenticator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private MemberReader memberReader;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn(DUMMY_ENCODED_PASSWORD);
        localAuthAuthenticator = new LocalAuthAuthenticator(passwordEncoder, localAuthReader, memberReader);
    }

    private LocalAuth storedAuth() {
        return AuthFixture.createLocalAuth(OWNER_MEMBER_KEY);
    }

    @Test
    @DisplayName("전화번호의 주인이 가진 비밀번호와 일치하면 그 회원을 돌려준다")
    void authenticate_returns_member_owning_the_phone() {
        // Given
        PhoneNumber phone = new PhoneNumber(MemberFixture.PHONE);
        Password password = AuthFixture.createPassword();
        Member owner = MemberFixture.createMemberWithKey(OWNER_MEMBER_KEY, MemberFixture.PHONE);

        given(memberReader.findOptionalByPhone(phone)).willReturn(Optional.of(owner));
        given(localAuthReader.findOptionalByMemberKey(OWNER_MEMBER_KEY)).willReturn(Optional.of(storedAuth()));
        given(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, AuthFixture.ENCODED_PASSWORD)).willReturn(true);

        // When
        Member result = localAuthAuthenticator.authenticate(phone, password);

        assertThat(result.getMemberKey()).isEqualTo(OWNER_MEMBER_KEY);
        assertThat(result.getPhone()).isEqualTo(MemberFixture.PHONE);
    }

    @Test
    @DisplayName("전화번호의 주인이 가진 비밀번호와 일치하지 않으면 INVALID_CREDENTIALS 예외가 발생한다")
    void authenticate_throws_when_password_mismatches() {
        // Given
        PhoneNumber phone = new PhoneNumber(MemberFixture.PHONE);
        Password password = AuthFixture.createPassword();
        Member owner = MemberFixture.createMemberWithKey(OWNER_MEMBER_KEY, MemberFixture.PHONE);

        given(memberReader.findOptionalByPhone(phone)).willReturn(Optional.of(owner));
        given(localAuthReader.findOptionalByMemberKey(OWNER_MEMBER_KEY)).willReturn(Optional.of(storedAuth()));
        given(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, AuthFixture.ENCODED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> localAuthAuthenticator.authenticate(phone, password))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("가입되지 않은 전화번호면 INVALID_CREDENTIALS 예외가 발생하고, 가입된 번호와 같이 비밀번호 해시 비교를 한 번 수행한다")
    void authenticate_throws_and_still_compares_hash_when_phone_unregistered() {
        // Given
        PhoneNumber phone = new PhoneNumber(MemberFixture.UNREGISTERED_PHONE);
        Password password = AuthFixture.createPassword();

        given(memberReader.findOptionalByPhone(phone)).willReturn(Optional.empty());

        assertThatThrownBy(() -> localAuthAuthenticator.authenticate(phone, password))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_CREDENTIALS);
        verify(passwordEncoder, times(1)).matches(AuthFixture.RAW_PASSWORD, DUMMY_ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("회원은 있지만 로컬 인증 정보가 없으면 해시 비교가 통과해도 INVALID_CREDENTIALS 예외가 발생한다")
    void authenticate_throws_when_local_auth_missing_even_if_hash_matches() {
        // Given
        PhoneNumber phone = new PhoneNumber(MemberFixture.PHONE);
        Password password = AuthFixture.createPassword();
        Member owner = MemberFixture.createMemberWithKey(OWNER_MEMBER_KEY, MemberFixture.PHONE);

        given(memberReader.findOptionalByPhone(phone)).willReturn(Optional.of(owner));
        given(localAuthReader.findOptionalByMemberKey(OWNER_MEMBER_KEY)).willReturn(Optional.empty());
        given(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, DUMMY_ENCODED_PASSWORD)).willReturn(true);

        assertThatThrownBy(() -> localAuthAuthenticator.authenticate(phone, password))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_CREDENTIALS);
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
