package com.recaring.auth.business;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.common.utils.MaskingUtils;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.business.FcmDeviceTokenService;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final TokenIssuer tokenIssuer;
    private final LocalAuthAuthenticator authAuthenticator;
    private final MemberReader memberReader;
    private final LocalAuthManager localAuthManager;
    private final LocalAuthReader localAuthReader;
    private final RefreshTokenWriter refreshTokenWriter;
    private final PhoneVerificationReader phoneVerificationReader;
    private final PhoneVerificationWriter phoneVerificationWriter;
    private final FcmDeviceTokenService fcmDeviceTokenService;

    // 인증 토큰은 조회 직후 소비해 1회용으로 만든다. 남겨두면 TTL 10분 동안 재사용할 수 있어,
    // SMS 한 건 값으로 이메일만 바꿔가며 중복 여부(ALREADY_REGISTERED_EMAIL)를 무제한 조회할 수 있다.
    // 성공 여부와 무관하게 소비하는 이유도 같다 — 열거는 실패를 반복하는 것이라 성공 시에만 지우면 막지 못한다.
    public void signUp(SignUpCommand command) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(command.smsToken());
        phoneVerificationWriter.deleteToken(command.smsToken());
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(command.password());
        localAuthManager.register(command.toNewLocalMember(phone, encodedPassword));
    }

    public Jwt signIn(LocalEmail email, Password password) {
        Member member = authAuthenticator.authenticate(email, password);
        return tokenIssuer.issue(member);
    }

    public String findEmail(String name, LocalDate birth, PhoneNumber phone) {
        Member member = memberReader.findAccount(name, birth, phone.value());
        return MaskingUtils.maskEmail(localAuthReader.findByMemberKey(member.getMemberKey()).getEmail());
    }

    public void resetPassword(String smsToken, Password password) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(smsToken);
        phoneVerificationWriter.deleteToken(smsToken);
        Member member = memberReader.findByPhone(phone);
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(password);
        localAuthManager.changePassword(member.getMemberKey(), encodedPassword.value());
    }

    public void signOut(String refreshToken, String fcmToken) {
        refreshTokenWriter.delete(refreshToken);
        if (fcmToken != null && !fcmToken.isBlank()) {
            fcmDeviceTokenService.delete(fcmToken);
        }
    }
}
