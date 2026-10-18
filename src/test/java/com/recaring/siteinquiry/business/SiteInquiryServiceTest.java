package com.recaring.siteinquiry.business;

import com.recaring.siteinquiry.business.command.SubmitSiteInquiryCommand;
import com.recaring.siteinquiry.fixture.SiteInquiryFixture;
import com.recaring.siteinquiry.implement.SiteInquiryAuthenticator;
import com.recaring.siteinquiry.implement.SiteInquiryManager;
import com.recaring.siteinquiry.implement.SiteInquiryOperatorNotifier;
import com.recaring.siteinquiry.implement.SiteInquiryRateLimitValidator;
import com.recaring.siteinquiry.implement.SiteInquiryReader;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteInquiryService 단위 테스트")
class SiteInquiryServiceTest {

    @InjectMocks
    private SiteInquiryService siteInquiryService;

    @Mock
    private SiteInquiryManager siteInquiryManager;

    @Mock
    private SiteInquiryReader siteInquiryReader;

    @Mock
    private SiteInquiryAuthenticator siteInquiryAuthenticator;

    @Mock
    private SiteInquiryRateLimitValidator siteInquiryRateLimitValidator;

    @Mock
    private SiteInquiryOperatorNotifier siteInquiryOperatorNotifier;

    @Test
    @DisplayName("등록 한도를 넘으면 저장하지 않고 알림도 보내지 않는다")
    void submit_over_quota_neither_saves_nor_notifies() {
        SubmitSiteInquiryCommand command = SiteInquiryFixture.createSecretSubmitCommand();
        willThrow(new AppException(ErrorType.SITE_INQUIRY_SUBMIT_RATE_LIMITED))
                .given(siteInquiryRateLimitValidator).validateSubmit(SiteInquiryFixture.CLIENT_IP);

        assertThatThrownBy(() -> siteInquiryService.submit(command, SiteInquiryFixture.CLIENT_IP))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_SUBMIT_RATE_LIMITED);
        then(siteInquiryManager).shouldHaveNoInteractions();
        then(siteInquiryOperatorNotifier).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("등록에 성공하면 운영자에게 접수 알림을 보낸다")
    void submit_notifies_operator() {
        SubmitSiteInquiryCommand command = SiteInquiryFixture.createSecretSubmitCommand();
        SiteInquiryInfo submitted = SiteInquiryFixture.createSubmittedInfo();
        given(siteInquiryAuthenticator.encode(command.password())).willReturn(SiteInquiryFixture.PASSWORD_HASH);
        given(siteInquiryManager.register(command, SiteInquiryFixture.PASSWORD_HASH)).willReturn(submitted);

        siteInquiryService.submit(command, SiteInquiryFixture.CLIENT_IP);

        then(siteInquiryOperatorNotifier).should().notifySubmitted(submitted);
    }
}
