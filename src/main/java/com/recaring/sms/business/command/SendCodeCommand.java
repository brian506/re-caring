package com.recaring.sms.business.command;

import com.recaring.sms.vo.PhoneNumber;

public record SendCodeCommand(PhoneNumber phone) {
}
