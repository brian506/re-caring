package com.recaring.care.business.command;

import com.recaring.sms.vo.PhoneNumber;

public record AddWardCommand(
        String requesterKey,
        PhoneNumber phoneNumber
) {
}
