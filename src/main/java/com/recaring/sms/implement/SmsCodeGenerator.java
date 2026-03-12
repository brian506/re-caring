package com.recaring.sms.implement;

import com.recaring.sms.vo.SmsCode;

import java.security.SecureRandom;

public class SmsCodeGenerator {

    private static final String CODE_FORMAT = "%06d";
    private static final int CODE_NUM_RANGE = 1_000_000;

    private SmsCodeGenerator() {}

    public static SmsCode generate() {
        String code = String.format(CODE_FORMAT, new SecureRandom().nextInt(CODE_NUM_RANGE));
        return new SmsCode(code);
    }
}
