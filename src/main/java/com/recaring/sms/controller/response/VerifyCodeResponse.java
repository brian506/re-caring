package com.recaring.sms.controller.response;

import com.recaring.sms.vo.VerifiedPhone;

public record VerifyCodeResponse(String verificationToken, boolean registered) {

    public static VerifyCodeResponse from(VerifiedPhone verifiedPhone) {
        return new VerifyCodeResponse(verifiedPhone.token(), verifiedPhone.registered());
    }
}
