package com.recaring.auth.implement.local;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.EncodedPassword;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthAuthenticator 단위 테스트")
class LocalAuthAuthenticatorTest {

    @InjectMocks
    private LocalAuthAuthenticator localAuthAuthenticator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("패스워드 인코딩 성공 - 평문 비밀번호를 BCrypt로 인코딩한다")
    void encodePassword_success() {
        // given
        Password rawPassword = AuthFixture.createPassword();
        String encodedValue = "$2a$10$encoded.password.hash";
        given(passwordEncoder.encode(rawPassword.value())).willReturn(encodedValue);

        // when
        EncodedPassword result = localAuthAuthenticator.encodePassword(rawPassword);

        // then
        assertThat(result.value()).isEqualTo(encodedValue);
    }

    @Test
    @DisplayName("인증 성공 - 올바른 이메일과 비밀번호로 회원 객체 반환")
    void authenticate_success() {
        // given
        LocalEmail email = AuthFixture.createLocalEmail();
        Password password = AuthFixture.createPassword();
        String memberKey = "member-key-123";
        Member expectedMember = MemberFixture.createMember();

        com.recaring.auth.dataaccess.entity.LocalAuth localAuth =
            com.recaring.auth.dataaccess.entity.LocalAuth.builder()
                .memberKey(memberKey)
                .email(email.value())
                .password(AuthFixture.ENCODED_PASSWORD)
                .build();

        given(localAuthReader.findByEmail(email.value())).willReturn(localAuth);
        given(passwordEncoder.matches(password.value(), AuthFixture.ENCODED_PASSWORD)).willReturn(true);
        given(memberReader.findByMemberKey(memberKey)).willReturn(expectedMember);

        // when
        Member result = localAuthAuthenticator.authenticate(email, password);

        // then
        assertThat(result.getMemberKey()).isEqualTo(expectedMember.getMemberKey());
    }

    @Test
    @DisplayName("인증 실패 - 잘못된 비밀번호로 AppException 발생")
    void authenticate_fail_with_wrong_password() {
        // given
        LocalEmail email = AuthFixture.createLocalEmail();
        Password password = AuthFixture.createPassword();
        String memberKey = "member-key-123";

        com.recaring.auth.dataaccess.entity.LocalAuth localAuth =
            com.recaring.auth.dataaccess.entity.LocalAuth.builder()
                .memberKey(memberKey)
                .email(email.value())
                .password(AuthFixture.ENCODED_PASSWORD)
                .build();

        given(localAuthReader.findByEmail(email.value())).willReturn(localAuth);
        given(passwordEncoder.matches(password.value(), AuthFixture.ENCODED_PASSWORD)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> localAuthAuthenticator.authenticate(email, password))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("인증 실패 - 존재하지 않는 이메일로 AppException 발생")
    void authenticate_fail_with_email_not_found() {
        // given
        LocalEmail email = AuthFixture.createLocalEmail();
        Password password = AuthFixture.createPassword();

        given(localAuthReader.findByEmail(email.value()))
            .willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT));

        // when & then
        assertThatThrownBy(() -> localAuthAuthenticator.authenticate(email, password))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }
}
