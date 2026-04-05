package com.recaring.care.business.command;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.sms.vo.PhoneNumber;

public record AddCaregiverCommand(
        String requesterKey,
        PhoneNumber phoneNumber,
        String wardMemberKey,
        CareRole careRole
) {
}
