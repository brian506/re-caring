package com.recaring.sms.controller.request;

import com.recaring.sms.business.command.VerifyCodeCommand;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import jakarta.validation.constraints.NotBlank;

public record VerifyCodeRequest(
        @NotBlank String phone,
        @NotBlank String code
) {
    public VerifyCodeCommand toCommand() {
        return new VerifyCodeCommand(new PhoneNumber(phone), new SmsCode(code));
    }
}
