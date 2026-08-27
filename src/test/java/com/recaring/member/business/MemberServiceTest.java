package com.recaring.member.business;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.vo.Password;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberWriter;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    private static final String MEMBER_KEY = MemberFixture.MEMBER_KEY;

    @InjectMocks
    private MemberService memberService;

    @Mock private MemberWriter memberWriter;
    @Mock private LocalAuthAuthenticator localAuthAuthenticator;
    @Mock private LocalAuthManager localAuthManager;

    @Test
    @DisplayName("비밀번호 변경은 현재 비밀번호를 확인한 뒤에야 새 비밀번호의 해시로 교체한다")
    void updateMyInfo_verifies_current_then_stores_encoded_new_password() {
        // Given
        given(localAuthAuthenticator.encodePassword(new Password(MemberFixture.NEW_PASSWORD)))
                .willReturn(AuthFixture.createEncodedPassword());

        // When
        memberService.updateMyInfo(MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH,
                MemberFixture.CURRENT_PASSWORD, MemberFixture.NEW_PASSWORD);

        // Then
        InOrder inOrder = inOrder(localAuthAuthenticator, localAuthManager);
        inOrder.verify(localAuthAuthenticator).verifyPassword(MEMBER_KEY, new Password(MemberFixture.CURRENT_PASSWORD));
        inOrder.verify(localAuthManager).changePassword(MEMBER_KEY, AuthFixture.ENCODED_PASSWORD);
        then(memberWriter).should().updateProfile(MEMBER_KEY, MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("새 비밀번호가 비어 있으면 프로필만 수정하고 비밀번호는 건드리지 않는다")
    void updateMyInfo_skips_password_flow_when_new_password_blank(String blankNewPassword) {
        // When
        memberService.updateMyInfo(MEMBER_KEY, MemberFixture.UPDATED_NAME, null, null, blankNewPassword);

        // Then
        then(memberWriter).should().updateProfile(MEMBER_KEY, MemberFixture.UPDATED_NAME, null);
        verifyNoInteractions(localAuthAuthenticator, localAuthManager);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 INVALID_PASSWORD 예외가 발생하고 비밀번호는 바뀌지 않는다")
    void updateMyInfo_throws_and_keeps_password_when_current_mismatch() {
        // Given
        willThrow(new AppException(ErrorType.INVALID_PASSWORD))
                .given(localAuthAuthenticator).verifyPassword(MEMBER_KEY, new Password(MemberFixture.WRONG_PASSWORD));

        // When / Then
        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_KEY, null, null,
                MemberFixture.WRONG_PASSWORD, MemberFixture.NEW_PASSWORD))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);
        then(localAuthAuthenticator).should(never()).encodePassword(any());
        then(localAuthManager).should(never()).changePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("현재 비밀번호를 생략하고 새 비밀번호만 보내면 PASSWORD_IS_NULL 예외가 발생한다")
    void updateMyInfo_throws_when_current_password_missing() {
        // When / Then
        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_KEY, null, null, null, MemberFixture.NEW_PASSWORD))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PASSWORD_IS_NULL);
        verifyNoInteractions(localAuthManager);
    }

    @Test
    @DisplayName("새 비밀번호가 형식에 맞지 않으면 INVALID_PASSWORD_FORMAT 예외가 발생하고 비밀번호는 바뀌지 않는다")
    void updateMyInfo_throws_when_new_password_has_invalid_format() {
        // When / Then
        assertThatThrownBy(() -> memberService.updateMyInfo(MEMBER_KEY, null, null,
                MemberFixture.CURRENT_PASSWORD, "onlyletters"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD_FORMAT);
        verifyNoInteractions(localAuthManager);
    }
}
