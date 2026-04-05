package com.recaring.care.controller.request;

import com.recaring.care.business.command.AddCaregiverCommand;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.sms.vo.PhoneNumber;
import jakarta.validation.constraints.NotBlank;

public record AddCaregiverRequest(
        @NotBlank String phoneNumber,
        @NotBlank String wardMemberKey
) {
    public AddCaregiverCommand toCommand(String requesterKey, CareRole careRole) {
        return new AddCaregiverCommand(requesterKey, new PhoneNumber(phoneNumber), wardMemberKey, careRole);
    }
}
