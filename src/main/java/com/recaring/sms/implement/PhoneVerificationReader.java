package com.recaring.sms.implement;

import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PhoneVerificationReader {

    private static final String CODE_KEY_PREFIX = "phone:verify:";
    private static final String TOKEN_KEY_PREFIX = "phone:token:";

    private final StringRedisTemplate redisTemplate;

    public SmsCode findCode(PhoneNumber phone) {
        String code = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + phone.value());
        if (code == null) {
            throw new AppException(ErrorType.EXPIRED_VERIFICATION_CODE);
        }
        return new SmsCode(code);
    }

    public PhoneNumber findPhoneByToken(String token) {
        String phone = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        if (phone == null) {
            throw new AppException(ErrorType.NOT_VERIFIED_PHONE);
        }
        return new PhoneNumber(phone);
    }
}
