package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.common.mapper.auth.AuthMapper;
import com.recaring.domain.member.implement.MemberWriter;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthManager 단위 테스트")
class LocalAuthManagerTest {

    @InjectMocks
    private LocalAuthManager localAuthManager;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private LocalAuthRepository localAuthRepository;

    @Mock
    private MemberWriter memberWriter;

    @Mock
    private AuthMapper mapper;

    @Test
    @DisplayName("회원가입 성공 - 새로운 회원을 등록한다")
    void register_success() {
        // given
        NewLocalMember newMember = NewLocalMember.builder()
            .email(AuthFixture.createLocalEmail())
            .password(AuthFixture.createEncodedPassword())
            .phone(SmsFixture.createPhoneNumber())
            .name("홍길동")
            .birth(LocalDate.of(1990, 1, 1))
            .gender(com.recaring.domain.member.dataaccess.entity.Gender.MALE)
            .role(com.recaring.domain.member.dataaccess.entity.MemberRole.GUARDIAN)
            .build();

        String memberKey = "member-key-123";
        LocalAuth localAuth = LocalAuth.builder()
            .memberKey(memberKey)
            .email(newMember.email().value())
            .password(newMember.password().value())
            .build();

        given(localAuthRepository.existsByEmail(newMember.email().value())).willReturn(false);
        given(memberWriter.registerLocalMember(newMember)).willReturn(memberKey);
        given(mapper.createLocalAuth(memberKey, newMember.email().value(), newMember.password().value()))
            .willReturn(localAuth);
        given(localAuthRepository.save(localAuth)).willReturn(localAuth);

        // when
        localAuthManager.register(newMember);

        // then
        then(memberWriter).should(times(1)).registerLocalMember(newMember);
        then(localAuthRepository).should(times(1)).save(any(LocalAuth.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일로 AppException 발생")
    void register_fail_with_duplicate_email() {
        // given
        NewLocalMember newMember = NewLocalMember.builder()
            .email(AuthFixture.createLocalEmail())
            .password(AuthFixture.createEncodedPassword())
            .phone(SmsFixture.createPhoneNumber())
            .name("홍길동")
            .birth(LocalDate.of(1990, 1, 1))
            .gender(com.recaring.domain.member.dataaccess.entity.Gender.MALE)
            .role(com.recaring.domain.member.dataaccess.entity.MemberRole.GUARDIAN)
            .build();

        given(localAuthRepository.existsByEmail(newMember.email().value())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> localAuthManager.register(newMember))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_EMAIL);

        then(memberWriter).should(times(0)).registerLocalMember(any());
    }

    @Test
    @DisplayName("비밀번호 변경 성공 - 주어진 memberKey의 비밀번호를 변경한다")
    void updatePassword_success() {
        // given
        String memberKey = "member-key-123";
        String newEncodedPassword = "$2a$10$new.encoded.password";
        LocalAuth existingAuth = LocalAuth.builder()
            .memberKey(memberKey)
            .email(AuthFixture.EMAIL)
            .password(AuthFixture.ENCODED_PASSWORD)
            .build();

        given(localAuthReader.findByMemberKey(memberKey)).willReturn(existingAuth);

        // when
        localAuthManager.changePassword(memberKey, newEncodedPassword);

        // then
        then(localAuthReader).should(times(1)).findByMemberKey(memberKey);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 존재하지 않는 회원으로 AppException 발생")
    void updatePassword_fail_member_not_found() {
        // given
        String memberKey = "non-existent-key";
        String newEncodedPassword = "$2a$10$new.encoded.password";

        given(localAuthReader.findByMemberKey(memberKey))
            .willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT));

        // when & then
        assertThatThrownBy(() -> localAuthManager.changePassword(memberKey, newEncodedPassword))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);
    }
}
