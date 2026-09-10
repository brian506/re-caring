package com.recaring.sms.vo;

public record VerifiedPhone(PhoneNumber phone, String token, boolean registered) {
}
