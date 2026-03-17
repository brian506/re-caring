package com.recaring.sms.business.command;

import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;

public record VerifyCodeCommand(PhoneNumber phone, SmsCode code) {
}
