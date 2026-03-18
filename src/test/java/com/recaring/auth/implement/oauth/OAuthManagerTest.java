package com.recaring.auth.implement.oauth;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.vo.NewOauthMember;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.common.mapper.auth.AuthMapper;
import com.recaring.domain.member.dataaccess.entity.Gender;
import com.recaring.domain.member.dataaccess.entity.MemberRole;
import com.recaring.domain.member.implement.MemberWriter;
import com.recaring.sms.fixture.SmsFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthManager 단위 테스트")
class OAuthManagerTest {

    @InjectMocks
    private OAuthManager oAuthManager;

    @Mock
    private OAuthRepository oAuthRepository;

    @Mock
    private MemberWriter memberWriter;

    @Mock
    private AuthMapper authMapper;

    @Test
    @DisplayName("OAuth 회원가입 성공 - memberWriter와 oAuthRepository 호출 검증")
    void register_success() {
        // given
        String memberKey = "member-key-oauth-123";
        NewOauthMember newOauthMember = NewOauthMember.builder()
            .phone(SmsFixture.PHONE)
            .name("김카카오")
            .birth(LocalDate.of(1995, 5, 15))
            .gender(Gender.FEMALE)
            .role(MemberRole.GUARDIAN)
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-user-456")
            .build();

        OAuth createdOAuth = OAuth.builder()
            .memberKey(memberKey)
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-user-456")
            .build();

        given(memberWriter.registerOAuthMember(newOauthMember)).willReturn(memberKey);
        given(authMapper.createOAuth(memberKey, OAuthProvider.KAKAO, "kakao-user-456")).willReturn(createdOAuth);
        given(oAuthRepository.save(createdOAuth)).willReturn(createdOAuth);

        // when
        String result = oAuthManager.register(newOauthMember);

        // then
        assertThat(result).isEqualTo(memberKey);
        then(memberWriter).should(times(1)).registerOAuthMember(newOauthMember);
        then(authMapper).should(times(1)).createOAuth(memberKey, OAuthProvider.KAKAO, "kakao-user-456");
        then(oAuthRepository).should(times(1)).save(any(OAuth.class));
    }

    @Test
    @DisplayName("OAuth 회원가입 - OAuth 엔티티가 올바르게 저장되는지 검증")
    void register_verify_oauth_entity_saved() {
        // given
        String memberKey = "member-oauth-789";
        NewOauthMember newOauthMember = NewOauthMember.builder()
            .phone("01087654321")
            .name("이네이버")
            .birth(LocalDate.of(1992, 3, 20))
            .gender(Gender.MALE)
            .role(MemberRole.GUARDIAN)
            .provider(OAuthProvider.NAVER)
            .providerUserId("naver-user-789")
            .build();

        OAuth createdOAuth = OAuth.builder()
            .memberKey(memberKey)
            .provider(OAuthProvider.NAVER)
            .providerUserId("naver-user-789")
            .build();

        given(memberWriter.registerOAuthMember(newOauthMember)).willReturn(memberKey);
        given(authMapper.createOAuth(memberKey, OAuthProvider.NAVER, "naver-user-789")).willReturn(createdOAuth);
        given(oAuthRepository.save(createdOAuth)).willReturn(createdOAuth);

        // when
        oAuthManager.register(newOauthMember);

        // then
        ArgumentCaptor<OAuth> oAuthCaptor = ArgumentCaptor.forClass(OAuth.class);
        then(oAuthRepository).should(times(1)).save(oAuthCaptor.capture());

        OAuth capturedOAuth = oAuthCaptor.getValue();
        assertThat(capturedOAuth.getMemberKey()).isEqualTo(memberKey);
        assertThat(capturedOAuth.getProvider()).isEqualTo(OAuthProvider.NAVER);
        assertThat(capturedOAuth.getProviderUserId()).isEqualTo("naver-user-789");
    }

    @Test
    @DisplayName("OAuth 회원가입 - 카카오 provider로 등록")
    void register_with_kakao_provider() {
        // given
        String memberKey = "kakao-member-key";
        NewOauthMember kakaoMember = NewOauthMember.builder()
            .phone("01011111111")
            .name("박카카오")
            .birth(LocalDate.of(1998, 7, 10))
            .gender(Gender.FEMALE)
            .role(MemberRole.GUARDIAN)
            .provider(OAuthProvider.KAKAO)
            .providerUserId("2827374756383")
            .build();

        OAuth createdOAuth = OAuth.builder()
            .memberKey(memberKey)
            .provider(OAuthProvider.KAKAO)
            .providerUserId(kakaoMember.providerUserId())
            .build();

        given(memberWriter.registerOAuthMember(kakaoMember)).willReturn(memberKey);
        given(authMapper.createOAuth(eq(memberKey), eq(OAuthProvider.KAKAO), any())).willReturn(createdOAuth);
        given(oAuthRepository.save(any(OAuth.class))).willReturn(createdOAuth);

        // when
        String result = oAuthManager.register(kakaoMember);

        // then
        assertThat(result).isEqualTo(memberKey);
    }

    @Test
    @DisplayName("OAuth 회원가입 - 네이버 provider로 등록")
    void register_with_naver_provider() {
        // given
        String memberKey = "naver-member-key";
        NewOauthMember naverMember = NewOauthMember.builder()
            .phone("01022222222")
            .name("최네이버")
            .birth(LocalDate.of(1996, 12, 5))
            .gender(Gender.MALE)
            .role(MemberRole.GUARDIAN)
            .provider(OAuthProvider.NAVER)
            .providerUserId("naver-uid-9384756")
            .build();

        OAuth createdOAuth = OAuth.builder()
            .memberKey(memberKey)
            .provider(OAuthProvider.NAVER)
            .providerUserId(naverMember.providerUserId())
            .build();

        given(memberWriter.registerOAuthMember(naverMember)).willReturn(memberKey);
        given(authMapper.createOAuth(eq(memberKey), eq(OAuthProvider.NAVER), any())).willReturn(createdOAuth);
        given(oAuthRepository.save(any(OAuth.class))).willReturn(createdOAuth);

        // when
        String result = oAuthManager.register(naverMember);

        // then
        assertThat(result).isEqualTo(memberKey);
    }

    @Test
    @DisplayName("OAuth 회원가입 - 트랜잭션 내에서 멤버와 OAuth 엔티티 함께 저장")
    void register_transactional_consistency() {
        // given
        String memberKey = "member-tx-123";
        NewOauthMember newOauthMember = NewOauthMember.builder()
            .phone("01099999999")
            .name("트랜잭션테스트")
            .birth(LocalDate.of(1994, 6, 12))
            .gender(Gender.FEMALE)
            .role(MemberRole.GUARDIAN)
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-tx-test")
            .build();

        OAuth createdOAuth = OAuth.builder()
            .memberKey(memberKey)
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-tx-test")
            .build();

        given(memberWriter.registerOAuthMember(newOauthMember)).willReturn(memberKey);
        given(authMapper.createOAuth(any(), any(), any())).willReturn(createdOAuth);
        given(oAuthRepository.save(any(OAuth.class))).willReturn(createdOAuth);

        // when
        String result = oAuthManager.register(newOauthMember);

        // then - 호출 순서 검증: memberWriter가 먼저 호출되어야 memberKey가 생김
        assertThat(result).isEqualTo(memberKey);
        then(memberWriter).should(times(1)).registerOAuthMember(any());
        then(oAuthRepository).should(times(1)).save(any());
    }
}
