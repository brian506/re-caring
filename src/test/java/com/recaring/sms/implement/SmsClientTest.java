package com.recaring.sms.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsClient 단위 테스트")
class SmsClientTest {

    @InjectMocks
    private SmsClient smsClient;

    @Mock
    private DefaultMessageService messageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smsClient, "sender", "01000000000");
    }

    @Test
    @DisplayName("발송 중 NullPointerException이 발생하면 SMS_SEND_FAILED로 변환한다")
    void sendVerificationCode_wraps_nullPointerException() {
        willThrow(new NullPointerException()).given(messageService).sendOne(any());

        assertThatThrownBy(() -> smsClient.sendVerificationCode("01012345678", "123456"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SMS_SEND_FAILED);
    }

    @Test
    @DisplayName("발송 중 NullPointerException 외의 예외가 발생해도 SMS_SEND_FAILED로 변환한다")
    void sendVerificationCode_wraps_any_other_exception() {
        willThrow(new IllegalStateException("coolsms gateway timeout")).given(messageService).sendOne(any());

        assertThatThrownBy(() -> smsClient.sendVerificationCode("01012345678", "123456"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SMS_SEND_FAILED);
    }
}
