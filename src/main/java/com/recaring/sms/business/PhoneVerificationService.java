package com.recaring.sms.business;

import com.recaring.member.implement.MemberReader;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.implement.SmsClient;
import com.recaring.sms.implement.SmsCodeGenerator;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.sms.vo.VerifiedPhone;
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
    private final MemberReader memberReader;

    public void sendCode(PhoneNumber phone) {
        SmsCode code = SmsCodeGenerator.generate();
        phoneVerificationWriter.add(phone, code);
        smsClient.sendVerificationCode(phone.value(), code.value());
    }

    public VerifiedPhone verifyCode(PhoneNumber phone, SmsCode code) {
        SmsCode storedCode = phoneVerificationReader.findCode(phone);
        if (!storedCode.matches(code)) {
            throw new AppException(ErrorType.INVALID_VERIFICATION_CODE);
        }
        String token = phoneVerificationWriter.verify(phone);
        return new VerifiedPhone(phone, token, memberReader.existsByPhone(phone));
    }
}
