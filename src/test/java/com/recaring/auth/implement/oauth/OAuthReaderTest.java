package com.recaring.auth.implement.oauth;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.vo.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthReader 단위 테스트")
class OAuthReaderTest {

    @InjectMocks
    private OAuthReader oAuthReader;

    @Mock
    private OAuthRepository oAuthRepository;

    @Test
    @DisplayName("OAuth 사용자 조회 성공 - provider와 providerMemberId로 조회")
    void findOAuthUser_success_found() {
        // given
        OAuthProvider provider = OAuthProvider.KAKAO;
        String providerMemberId = "kakao-user-123";
        String memberKey = "member-key-123";

        OAuth oAuth = OAuth.builder()
            .memberKey(memberKey)
            .provider(provider)
            .providerMemberId(providerMemberId)
            .build();

        given(oAuthRepository.findOAuthUser(provider, providerMemberId))
            .willReturn(Optional.of(oAuth));

        // when
        Optional<OAuth> result = oAuthReader.findOAuthUser(provider, providerMemberId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getMemberKey()).isEqualTo(memberKey);
        assertThat(result.get().getProvider()).isEqualTo(provider);
        assertThat(result.get().getProviderMemberId()).isEqualTo(providerMemberId);
    }

    @Test
    @DisplayName("OAuth 사용자 조회 실패 - 존재하지 않는 사용자")
    void findOAuthUser_success_not_found() {
        // given
        OAuthProvider provider = OAuthProvider.NAVER;
        String providerMemberId = "naver-user-nonexistent";

        given(oAuthRepository.findOAuthUser(provider, providerMemberId))
            .willReturn(Optional.empty());

        // when
        Optional<OAuth> result = oAuthReader.findOAuthUser(provider, providerMemberId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("OAuth 사용자 조회 - 카카오 provider")
    void findOAuthUser_with_kakao_provider() {
        // given
        OAuthProvider provider = OAuthProvider.KAKAO;
        String providerMemberId = "2928374756";

        OAuth oAuth = OAuth.builder()
            .memberKey("member-kakao")
            .provider(provider)
            .providerMemberId(providerMemberId)
            .build();

        given(oAuthRepository.findOAuthUser(OAuthProvider.KAKAO, providerMemberId))
            .willReturn(Optional.of(oAuth));

        // when
        Optional<OAuth> result = oAuthReader.findOAuthUser(provider, providerMemberId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getProvider()).isEqualTo(OAuthProvider.KAKAO);
    }

    @Test
    @DisplayName("OAuth 사용자 조회 - 네이버 provider")
    void findOAuthUser_with_naver_provider() {
        // given
        OAuthProvider provider = OAuthProvider.NAVER;
        String providerMemberId = "naver-id-987654";

        OAuth oAuth = OAuth.builder()
            .memberKey("member-naver")
            .provider(provider)
            .providerMemberId(providerMemberId)
            .build();

        given(oAuthRepository.findOAuthUser(OAuthProvider.NAVER, providerMemberId))
            .willReturn(Optional.of(oAuth));

        // when
        Optional<OAuth> result = oAuthReader.findOAuthUser(provider, providerMemberId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getProvider()).isEqualTo(OAuthProvider.NAVER);
    }

    @Test
    @DisplayName("OAuth 사용자 조회 - 같은 providerMemberId로 여러 provider 구분")
    void findOAuthUser_same_provider_user_id_different_provider() {
        // given
        String sameProviderId = "shared-id-123";

        OAuth kakaoOAuth = OAuth.builder()
            .memberKey("member-kakao")
            .provider(OAuthProvider.KAKAO)
            .providerMemberId(sameProviderId)
            .build();

        OAuth naverOAuth = OAuth.builder()
            .memberKey("member-naver")
            .provider(OAuthProvider.NAVER)
            .providerMemberId(sameProviderId)
            .build();

        given(oAuthRepository.findOAuthUser(OAuthProvider.KAKAO, sameProviderId))
            .willReturn(Optional.of(kakaoOAuth));
        given(oAuthRepository.findOAuthUser(OAuthProvider.NAVER, sameProviderId))
            .willReturn(Optional.of(naverOAuth));

        // when
        Optional<OAuth> kakaoResult = oAuthReader.findOAuthUser(OAuthProvider.KAKAO, sameProviderId);
        Optional<OAuth> naverResult = oAuthReader.findOAuthUser(OAuthProvider.NAVER, sameProviderId);

        // then
        assertThat(kakaoResult).isPresent();
        assertThat(kakaoResult.get().getMemberKey()).isEqualTo("member-kakao");
        assertThat(naverResult).isPresent();
        assertThat(naverResult.get().getMemberKey()).isEqualTo("member-naver");
    }
}
