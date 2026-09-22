package com.recaring.sms.business;

import com.recaring.member.implement.MemberReader;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.implement.SmsClient;
import com.recaring.sms.implement.SmsRateLimitValidator;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.sms.vo.VerifiedPhone;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhoneVerificationService 단위 테스트")
class PhoneVerificationServiceTest {

    @InjectMocks
    private PhoneVerificationService phoneVerificationService;

    @Mock
    private PhoneVerificationWriter phoneVerificationWriter;

    @Mock
    private PhoneVerificationReader phoneVerificationReader;

    @Mock
    private SmsClient smsClient;

    @Mock
    private MemberReader memberReader;

    @Mock
    private SmsRateLimitValidator smsRateLimitValidator;

    @Test
    @DisplayName("발송 전에 한도를 먼저 검사한 뒤 저장한 코드와 같은 코드를 SMS로 발송한다")
    void sendCode_checks_quota_before_storing_and_sending() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();

        phoneVerificationService.sendCode(phone);

        ArgumentCaptor<SmsCode> storedCode = ArgumentCaptor.forClass(SmsCode.class);
        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        InOrder inOrder = inOrder(smsRateLimitValidator, phoneVerificationWriter, smsClient);
        inOrder.verify(smsRateLimitValidator).validate(phone);
        inOrder.verify(phoneVerificationWriter).add(eq(phone), storedCode.capture());
        inOrder.verify(smsClient).sendVerificationCode(eq(SmsFixture.PHONE), sentCode.capture());
        assertThat(sentCode.getValue()).isEqualTo(storedCode.getValue().value());
    }

    @Test
    @DisplayName("한도를 넘으면 인증 코드를 저장하지도 발송하지도 않는다")
    void sendCode_does_not_store_or_send_when_quota_exceeded() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        willThrow(new AppException(ErrorType.SMS_SEND_QUOTA_EXCEEDED))
                .given(smsRateLimitValidator).validate(phone);

        assertThatThrownBy(() -> phoneVerificationService.sendCode(phone))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SMS_SEND_QUOTA_EXCEEDED);

        then(phoneVerificationWriter).shouldHaveNoInteractions();
        then(smsClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("올바른 인증 코드 입력 시 검증 토큰이 반환된다")
    void verifyCode_success() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        SmsCode code = SmsFixture.createSmsCode();
        String expectedToken = "some-verification-token";

        given(phoneVerificationReader.findCode(phone)).willReturn(SmsFixture.createSmsCode());
        given(phoneVerificationWriter.verify(phone)).willReturn(expectedToken);
        given(memberReader.existsByPhone(phone)).willReturn(false);

        VerifiedPhone result = phoneVerificationService.verifyCode(phone, code);

        assertThat(result.token()).isEqualTo(expectedToken);
        assertThat(result.registered()).isFalse();
        then(phoneVerificationWriter).should(times(1)).verify(phone);
    }

    @Test
    @DisplayName("잘못된 인증 코드 입력 시 INVALID_VERIFICATION_CODE 예외가 발생한다")
    void verifyCode_fail_when_code_mismatch() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        SmsCode wrongCode = SmsFixture.createSmsCode("999999");

        given(phoneVerificationReader.findCode(phone)).willReturn(SmsFixture.createSmsCode());

        assertThatThrownBy(() -> phoneVerificationService.verifyCode(phone, wrongCode))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_VERIFICATION_CODE);
    }

    @Test
    @DisplayName("만료된 인증 코드라면 EXPIRED_VERIFICATION_CODE 예외가 발생한다")
    void verifyCode_fail_when_code_expired() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        SmsCode code = SmsFixture.createSmsCode();

        given(phoneVerificationReader.findCode(phone))
                .willThrow(new AppException(ErrorType.EXPIRED_VERIFICATION_CODE));

        assertThatThrownBy(() -> phoneVerificationService.verifyCode(phone, code))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_VERIFICATION_CODE);
    }
}
