package com.recaring.sms.controller.request;

import com.recaring.sms.business.command.SendCodeCommand;
import com.recaring.sms.vo.PhoneNumber;
import jakarta.validation.constraints.NotBlank;

public record SendCodeRequest(
        @NotBlank String phone
) {
    public SendCodeCommand toCommand() {
        return new SendCodeCommand(new PhoneNumber(phone));
    }
}
