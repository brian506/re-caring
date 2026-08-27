package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.implement.MemberWriter;
import com.recaring.member.implement.MembersTermsAgreementWriter;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthManager 단위 테스트")
class LocalAuthManagerTest {

    private static final String NEW_MEMBER_KEY = "new-member-key-uuid";
    private static final String NEW_ENCODED_PASSWORD = "$2a$10$new.encoded.password";

    @InjectMocks
    private LocalAuthManager localAuthManager;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private LocalAuthRepository localAuthRepository;

    @Mock
    private MemberWriter memberWriter;

    @Mock
    private MembersTermsAgreementWriter termsAgreementWriter;

    private NewLocalMember newLocalMember() {
        return NewLocalMember.builder()
                .email(AuthFixture.createLocalEmail())
                .password(AuthFixture.createEncodedPassword())
                .phone(SmsFixture.createPhoneNumber())
                .name("홍길동")
                .birth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.GUARDIAN)
                .build();
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 회원·인증정보·약관동의가 모두 등록된다")
    void register_persists_member_auth_and_terms() {
        // Given
        NewLocalMember newMember = newLocalMember();
        given(localAuthRepository.existsByEmail(AuthFixture.EMAIL)).willReturn(false);
        given(memberWriter.registerLocalMember(newMember)).willReturn(NEW_MEMBER_KEY);

        // When
        localAuthManager.register(newMember);

        // Then
        ArgumentCaptor<LocalAuth> captor = ArgumentCaptor.forClass(LocalAuth.class);
        then(localAuthRepository).should(times(1)).save(captor.capture());
        LocalAuth saved = captor.getValue();

        assertThat(saved.getMemberKey()).isEqualTo(NEW_MEMBER_KEY);
        assertThat(saved.getEmail()).isEqualTo(AuthFixture.EMAIL);
        assertThat(saved.getPassword()).isEqualTo(AuthFixture.ENCODED_PASSWORD);

        then(termsAgreementWriter).should(times(1)).register(NEW_MEMBER_KEY);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 INVALID_EMAIL 예외가 발생하고 아무것도 저장하지 않는다")
    void register_throws_and_writes_nothing_when_email_already_exists() {
        // Given
        NewLocalMember newMember = newLocalMember();
        given(localAuthRepository.existsByEmail(AuthFixture.EMAIL)).willReturn(true);

        assertThatThrownBy(() -> localAuthManager.register(newMember))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_EMAIL);

        then(memberWriter).should(never()).registerLocalMember(any());
        then(localAuthRepository).should(never()).save(any(LocalAuth.class));
        then(termsAgreementWriter).should(never()).register(anyString());
    }

    @Test
    @DisplayName("비밀번호를 변경하면 해당 회원의 인증정보에 새 비밀번호가 반영된다")
    void changePassword_replaces_stored_password() {
        // Given
        LocalAuth existingAuth = LocalAuth.builder()
                .memberKey(AuthFixture.MEMBER_KEY)
                .email(AuthFixture.EMAIL)
                .password(AuthFixture.ENCODED_PASSWORD)
                .build();
        given(localAuthReader.findByMemberKey(AuthFixture.MEMBER_KEY)).willReturn(existingAuth);

        // When
        localAuthManager.changePassword(AuthFixture.MEMBER_KEY, NEW_ENCODED_PASSWORD);

        assertThat(existingAuth.getPassword()).isEqualTo(NEW_ENCODED_PASSWORD);
        assertThat(existingAuth.getEmail()).isEqualTo(AuthFixture.EMAIL);
        assertThat(existingAuth.getMemberKey()).isEqualTo(AuthFixture.MEMBER_KEY);
    }
}
