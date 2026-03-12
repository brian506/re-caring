package com.recaring.sms.business;

import com.recaring.sms.business.command.SendCodeCommand;
import com.recaring.sms.business.command.VerifyCodeCommand;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.implement.SmsClient;
import com.recaring.sms.implement.SmsCodeGenerator;
import com.recaring.sms.vo.SmsCode;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneVerificationWriter phoneVerificationWriter;
    private final PhoneVerificationReader phoneVerificationReader;
    private final SmsClient smsClient;

    public void sendCode(SendCodeCommand command) {
        SmsCode code = SmsCodeGenerator.generate();
        phoneVerificationWriter.add(command.phone(), code);
        smsClient.sendVerificationCode(command.phone().value(), code.value());
    }

    public void verifyCode(VerifyCodeCommand command) {
        SmsCode storedCode = phoneVerificationReader.findCode(command.phone());
        if (!storedCode.matches(command.code())) {
            throw new AppException(ErrorType.INVALID_VERIFICATION_CODE);
        }
        phoneVerificationWriter.verify(command.phone());
    }
}
