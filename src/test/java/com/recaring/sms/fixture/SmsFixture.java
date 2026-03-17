package com.recaring.sms.fixture;

import com.recaring.sms.business.command.SendCodeCommand;
import com.recaring.sms.business.command.VerifyCodeCommand;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;

public class SmsFixture {

    public static final String PHONE = "01012345678";
    public static final String CODE = "123456";

    /** 기본 PhoneNumber VO 생성 */
    public static PhoneNumber createPhoneNumber() {
        return new PhoneNumber(PHONE);
    }

    /** 전화번호를 지정하여 PhoneNumber VO 생성 */
    public static PhoneNumber createPhoneNumber(String phone) {
        return new PhoneNumber(phone);
    }

    /** 기본 SmsCode VO 생성 */
    public static SmsCode createSmsCode() {
        return new SmsCode(CODE);
    }

    /** 인증코드를 지정하여 SmsCode VO 생성 */
    public static SmsCode createSmsCode(String code) {
        return new SmsCode(code);
    }

    /** 기본 SendCodeCommand 생성 */
    public static SendCodeCommand createSendCodeCommand() {
        return new SendCodeCommand(createPhoneNumber());
    }

    /** 기본 VerifyCodeCommand 생성 */
    public static VerifyCodeCommand createVerifyCodeCommand() {
        return new VerifyCodeCommand(createPhoneNumber(), createSmsCode());
    }

    /** 코드를 지정하여 VerifyCodeCommand 생성 */
    public static VerifyCodeCommand createVerifyCodeCommand(String code) {
        return new VerifyCodeCommand(createPhoneNumber(), new SmsCode(code));
    }
}
