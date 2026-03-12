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
    private static final String VERIFIED_KEY_PREFIX = "phone:verified:";

    private final StringRedisTemplate redisTemplate;

    public SmsCode findCode(PhoneNumber phone) {
        String code = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + phone.value());
        if (code == null) {
            throw new AppException(ErrorType.EXPIRED_VERIFICATION_CODE);
        }
        return new SmsCode(code);
    }

    public void checkVerified(PhoneNumber phone) {
        String verified = redisTemplate.opsForValue().get(VERIFIED_KEY_PREFIX + phone.value());
        if (verified == null) {
            throw new AppException(ErrorType.NOT_VERIFIED_PHONE);
        }
    }
}
