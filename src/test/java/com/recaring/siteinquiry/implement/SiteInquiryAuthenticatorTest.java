package com.recaring.siteinquiry.implement;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.repository.SiteInquiryRepository;
import com.recaring.siteinquiry.fixture.SiteInquiryFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteInquiryAuthenticator 단위 테스트")
class SiteInquiryAuthenticatorTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private SiteInquiryRepository siteInquiryRepository;

    @Mock
    private SiteInquiryRateLimitValidator rateLimitValidator;

    private SiteInquiryAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new SiteInquiryAuthenticator(passwordEncoder, siteInquiryRepository, rateLimitValidator);
    }

    @Test
    @DisplayName("공개글은 비밀번호 없이 통과하고 시도 횟수를 쓰지 않는다")
    void public_inquiry_passes_without_password_or_attempt() {
        given(siteInquiryRepository.findByInquiryKey(SiteInquiryFixture.INQUIRY_KEY))
                .willReturn(Optional.of(SiteInquiryFixture.createPublicInquiry(SiteInquiryFixture.TITLE)));

        assertThatCode(() -> authenticator.authenticate(SiteInquiryFixture.INQUIRY_KEY, null))
                .doesNotThrowAnyException();
        then(rateLimitValidator).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("비밀글은 등록할 때의 비밀번호와 같으면 통과하고 시도 횟수를 되돌린다")
    void secret_inquiry_passes_with_registered_password_and_releases_attempt() {
        givenSecretInquiryRegisteredWith(SiteInquiryFixture.PASSWORD);

        assertThatCode(() -> authenticator.authenticate(SiteInquiryFixture.INQUIRY_KEY, SiteInquiryFixture.PASSWORD))
                .doesNotThrowAnyException();
        then(rateLimitValidator).should().releaseViewAttempt(SiteInquiryFixture.INQUIRY_KEY);
    }

    @Test
    @DisplayName("비밀글을 틀린 비밀번호로 조회하면 거부되고 시도가 실패로 남는다")
    void secret_inquiry_rejects_wrong_password_and_records_failure() {
        givenSecretInquiryRegisteredWith(SiteInquiryFixture.PASSWORD);

        assertThatThrownBy(() -> authenticator.authenticate(SiteInquiryFixture.INQUIRY_KEY, SiteInquiryFixture.WRONG_PASSWORD))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_PASSWORD_MISMATCH);
        then(rateLimitValidator).should().reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY);
        then(rateLimitValidator).should(never()).releaseViewAttempt(SiteInquiryFixture.INQUIRY_KEY);
    }

    @Test
    @DisplayName("비밀글을 비밀번호 없이 조회하면 거부되고 시도가 실패로 남는다")
    void secret_inquiry_rejects_missing_password_and_records_failure() {
        givenSecretInquiryRegisteredWith(SiteInquiryFixture.PASSWORD);

        assertThatThrownBy(() -> authenticator.authenticate(SiteInquiryFixture.INQUIRY_KEY, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_PASSWORD_MISMATCH);
        then(rateLimitValidator).should().reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY);
        then(rateLimitValidator).should(never()).releaseViewAttempt(SiteInquiryFixture.INQUIRY_KEY);
    }

    @Test
    @DisplayName("잠긴 문의는 맞는 비밀번호를 넣어도 거부된다")
    void locked_inquiry_rejects_correct_password() {
        givenSecretInquiryRegisteredWith(SiteInquiryFixture.PASSWORD);
        willThrow(new AppException(ErrorType.SITE_INQUIRY_VIEW_LOCKED))
                .given(rateLimitValidator).reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY);

        assertThatThrownBy(() -> authenticator.authenticate(SiteInquiryFixture.INQUIRY_KEY, SiteInquiryFixture.PASSWORD))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_VIEW_LOCKED);
    }

    private void givenSecretInquiryRegisteredWith(String rawPassword) {
        SiteInquiry secret = SiteInquiryFixture.createSecretInquiry(passwordEncoder.encode(rawPassword));
        given(siteInquiryRepository.findByInquiryKey(SiteInquiryFixture.INQUIRY_KEY)).willReturn(Optional.of(secret));
    }
}
