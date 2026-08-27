package com.recaring.auth.implement.oauth;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthLinkValidator 단위 테스트")
class OAuthLinkValidatorTest {

    @InjectMocks
    private OAuthLinkValidator oAuthLinkValidator;

    @Mock
    private OAuthReader oAuthReader;

    @Test
    @DisplayName("연동 가능 - 아직 연동되지 않은 provider면 통과한다")
    void validateLinkable_passes_when_not_linked() {
        // given
        String memberKey = "member-key";
        OAuthProvider provider = OAuthProvider.KAKAO;
        given(oAuthReader.isLinked(memberKey, provider)).willReturn(false);

        assertThatCode(() -> oAuthLinkValidator.validateLinkable(memberKey, provider))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("연동 불가 - 이미 연동된 provider면 OAUTH_ALREADY_LINKED 예외가 발생한다")
    void validateLinkable_throws_when_already_linked() {
        // given
        String memberKey = "member-key";
        OAuthProvider provider = OAuthProvider.KAKAO;
        given(oAuthReader.isLinked(memberKey, provider)).willReturn(true);

        assertThatThrownBy(() -> oAuthLinkValidator.validateLinkable(memberKey, provider))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.OAUTH_ALREADY_LINKED);
    }
}
