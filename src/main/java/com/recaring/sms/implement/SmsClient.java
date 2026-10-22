package com.recaring.sms.implement;

import com.recaring.common.utils.MaskingUtils;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsClient {

    private static final String SEND_TIMER_NAME = "sms.send";

    private final DefaultMessageService messageService;
    private final MeterRegistry meterRegistry;

    @Value("${coolsms.sender}")
    private String sender;

    public void sendVerificationCode(String phone, String code) {
        Message message = new Message();
        message.setFrom(sender);
        message.setTo(phone);
        message.setText("[re;caRing] 인증번호 [" + code + "]를 입력해주세요.");

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            messageService.sendOne(new SingleMessageSendingRequest(message));
            sample.stop(sendTimer("SUCCESS", "none"));
        } catch (Exception e) {
            sample.stop(sendTimer("FAILURE", e.getClass().getSimpleName()));
            log.warn("[SmsSend : Failed]: phone={} | type={}", MaskingUtils.maskPhone(phone), e.getClass().getName());
            throw new AppException(ErrorType.SMS_SEND_FAILED);
        }
    }

    private Timer sendTimer(String outcome, String exception) {
        return Timer.builder(SEND_TIMER_NAME)
                .tag("outcome", outcome)
                .tag("exception", exception)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }
}
