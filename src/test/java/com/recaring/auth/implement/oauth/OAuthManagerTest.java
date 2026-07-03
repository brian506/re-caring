package com.recaring.auth.implement.oauth;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthManager 단위 테스트")
class OAuthManagerTest {

    @InjectMocks
    private OAuthManager oAuthManager;

    @Mock
    private OAuthRepository oAuthRepository;

    @Mock
    private OAuthLinkValidator oAuthLinkValidator;

    @Test
    @DisplayName("연동 성공 - 검증 통과 후 OAuth 엔티티를 저장한다")
    void link_success() {
        // given
        String memberKey = "member-key-123";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String providerMemberId = "kakao-user-456";

        // when
        oAuthManager.link(memberKey, provider, providerMemberId);

        // then
        then(oAuthLinkValidator).should(times(1)).validateLinkable(memberKey, provider);

        ArgumentCaptor<OAuth> captor = ArgumentCaptor.forClass(OAuth.class);
        then(oAuthRepository).should(times(1)).save(captor.capture());
        OAuth saved = captor.getValue();
        assertThat(saved.getMemberKey()).isEqualTo(memberKey);
        assertThat(saved.getProvider()).isEqualTo(provider);
        assertThat(saved.getProviderMemberId()).isEqualTo(providerMemberId);
    }

    @Test
    @DisplayName("연동 실패 - 이미 연동된 provider면 예외가 발생하고 저장하지 않는다")
    void link_fails_when_already_linked() {
        // given
        String memberKey = "member-key-123";
        OAuthProvider provider = OAuthProvider.NAVER;
        willThrow(new AppException(ErrorType.OAUTH_ALREADY_LINKED))
                .given(oAuthLinkValidator).validateLinkable(memberKey, provider);

        // when & then
        assertThatThrownBy(() -> oAuthManager.link(memberKey, provider, "naver-user-789"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.OAUTH_ALREADY_LINKED);

        then(oAuthRepository).should(never()).save(any(OAuth.class));
    }
}
