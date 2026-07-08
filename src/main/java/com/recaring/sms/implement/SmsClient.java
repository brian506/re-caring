package com.recaring.sms.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
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

    private final DefaultMessageService messageService;

    @Value("${coolsms.sender}")
    private String sender;

    public void sendVerificationCode(String phone, String code) {
        Message message = new Message();
        message.setFrom(sender);
        message.setTo(phone);
        message.setText("[re;caRing] 인증번호 [" + code + "]를 입력해주세요.");

        try {
            messageService.sendOne(new SingleMessageSendingRequest(message));
        } catch (Exception e) {
            log.error("[SMS 발송 : 실패]: phone={} | type={} | error={}", phone, e.getClass().getSimpleName(), e.getMessage());
            throw new AppException(ErrorType.SMS_SEND_FAILED);
        }
    }
}
