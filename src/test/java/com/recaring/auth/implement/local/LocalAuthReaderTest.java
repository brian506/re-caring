package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthReader 단위 테스트")
class LocalAuthReaderTest {

    @InjectMocks
    private LocalAuthReader localAuthReader;

    @Mock
    private LocalAuthRepository authRepository;

    @Test
    @DisplayName("memberKey로 조회 성공 - LocalAuth 엔티티 반환")
    void findByMemberKey_success() {
        // given
        String memberKey = "member-key-123";
        LocalAuth localAuth = LocalAuth.builder()
            .memberKey(memberKey)
            .email(AuthFixture.EMAIL)
            .password(AuthFixture.ENCODED_PASSWORD)
            .build();

        given(authRepository.findByMemberKey(memberKey)).willReturn(Optional.of(localAuth));

        // when
        LocalAuth result = localAuthReader.findByMemberKey(memberKey);

        // then
        assertThat(result.getMemberKey()).isEqualTo(memberKey);
        assertThat(result.getEmail()).isEqualTo(AuthFixture.EMAIL);
    }

    @Test
    @DisplayName("memberKey로 조회 실패 - NOT_FOUND_ACCOUNT 예외 발생")
    void findByMemberKey_fail_not_found() {
        // given
        String memberKey = "non-existent-key";
        given(authRepository.findByMemberKey(memberKey)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> localAuthReader.findByMemberKey(memberKey))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

    @Test
    @DisplayName("이메일로 조회 성공 - LocalAuth 엔티티 반환")
    void findByEmail_success() {
        // given
        LocalAuth localAuth = LocalAuth.builder()
            .memberKey("member-key-123")
            .email(AuthFixture.EMAIL)
            .password(AuthFixture.ENCODED_PASSWORD)
            .build();

        given(authRepository.findByEmail(AuthFixture.EMAIL)).willReturn(Optional.of(localAuth));

        // when
        LocalAuth result = localAuthReader.findByEmail(AuthFixture.EMAIL);

        // then
        assertThat(result.getEmail()).isEqualTo(AuthFixture.EMAIL);
        assertThat(result.getPassword()).isEqualTo(AuthFixture.ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("이메일로 조회 실패 - NOT_FOUND_ACCOUNT 예외 발생")
    void findByEmail_fail_not_found() {
        // given
        String email = "nonexistent@example.com";
        given(authRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> localAuthReader.findByEmail(email))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }

    @Test
    @DisplayName("이메일로 조회 - 특수 문자가 포함된 이메일")
    void findByEmail_with_special_characters() {
        // given
        String specialEmail = "user+tag@example.co.kr";
        LocalAuth localAuth = LocalAuth.builder()
            .memberKey("member-key-456")
            .email(specialEmail)
            .password(AuthFixture.ENCODED_PASSWORD)
            .build();

        given(authRepository.findByEmail(specialEmail)).willReturn(Optional.of(localAuth));

        // when
        LocalAuth result = localAuthReader.findByEmail(specialEmail);

        // then
        assertThat(result.getEmail()).isEqualTo(specialEmail);
    }
}
