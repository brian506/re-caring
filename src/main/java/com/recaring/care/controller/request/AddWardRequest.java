package com.recaring.care.controller.request;

import com.recaring.care.business.command.AddWardCommand;
import com.recaring.sms.vo.PhoneNumber;
import jakarta.validation.constraints.NotBlank;

public record AddWardRequest(
        @NotBlank String phoneNumber
) {
    public AddWardCommand toCommand(String requesterKey) {
        return new AddWardCommand(requesterKey, new PhoneNumber(phoneNumber));
    }
}
