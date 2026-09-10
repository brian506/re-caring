package com.recaring.sms.implement;

import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PhoneVerificationWriter {

    private static final String CODE_KEY_PREFIX = "phone:verify:";
    private static final String TOKEN_KEY_PREFIX = "phone:token:";
    private static final long CODE_TTL_MINUTES = 5;
    private static final long TOKEN_TTL_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;

    public void add(PhoneNumber phone, SmsCode code) {
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone.value(), code.value(), CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public String verify(PhoneNumber phone) {
        String token = UUID.randomUUID().toString();
        redisTemplate.delete(CODE_KEY_PREFIX + phone.value());
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, phone.value(), TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    // 조회와 삭제를 GETDEL 한 커맨드로 처리한다. GET 후 DEL로 나누면 같은 토큰을 쥔 동시 요청이
    // 전부 GET을 통과한 뒤에야 DEL이 도달해, SMS 한 건으로 여러 번 시도할 수 있는 창이 열린다.
    public PhoneNumber consumePhoneByToken(String token) {
        String phone = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + token);
        if (phone == null) {
            throw new AppException(ErrorType.NOT_VERIFIED_PHONE);
        }
        return new PhoneNumber(phone);
    }
}
