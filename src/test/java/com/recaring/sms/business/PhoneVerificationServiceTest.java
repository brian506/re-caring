package com.recaring.sms.business;

import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.business.command.SendCodeCommand;
import com.recaring.sms.business.command.VerifyCodeCommand;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.implement.SmsClient;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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

    @Test
    @DisplayName("인증 코드 발송 시 코드가 저장되고 SMS가 발송된다")
    void sendCode_success() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        SendCodeCommand command = SmsFixture.createSendCodeCommand();

        phoneVerificationService.sendCode(command);

        then(phoneVerificationWriter).should(times(1)).add(eq(phone), any(SmsCode.class));
        then(smsClient).should(times(1)).sendVerificationCode(eq(SmsFixture.PHONE), any(String.class));
    }

    @Test
    @DisplayName("올바른 인증 코드 입력 시 검증 토큰이 반환된다")
    void verifyCode_success() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        VerifyCodeCommand command = SmsFixture.createVerifyCodeCommand();
        String expectedToken = "some-verification-token";

        given(phoneVerificationReader.findCode(phone)).willReturn(SmsFixture.createSmsCode());
        given(phoneVerificationWriter.verify(phone)).willReturn(expectedToken);

        String result = phoneVerificationService.verifyCode(command);

        assertThat(result).isEqualTo(expectedToken);
        then(phoneVerificationWriter).should(times(1)).verify(phone);
    }

    @Test
    @DisplayName("잘못된 인증 코드 입력 시 INVALID_VERIFICATION_CODE 예외가 발생한다")
    void verifyCode_fail_when_code_mismatch() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        VerifyCodeCommand command = SmsFixture.createVerifyCodeCommand("999999");

        given(phoneVerificationReader.findCode(phone)).willReturn(SmsFixture.createSmsCode());

        assertThatThrownBy(() -> phoneVerificationService.verifyCode(command))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_VERIFICATION_CODE);
    }

    @Test
    @DisplayName("만료된 인증 코드라면 EXPIRED_VERIFICATION_CODE 예외가 발생한다")
    void verifyCode_fail_when_code_expired() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        VerifyCodeCommand command = SmsFixture.createVerifyCodeCommand();

        given(phoneVerificationReader.findCode(phone))
                .willThrow(new AppException(ErrorType.EXPIRED_VERIFICATION_CODE));

        assertThatThrownBy(() -> phoneVerificationService.verifyCode(command))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_VERIFICATION_CODE);
    }
}
