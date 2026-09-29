package com.recaring.member.business;

import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.member.controller.response.MyInfoResponse;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MemberWriter;
import com.recaring.member.implement.MembersTermsAgreementReader;
import com.recaring.member.implement.MemberWithdrawalManager;
import com.recaring.member.vo.ProfileAvatarCode;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final LocalAuthAuthenticator localAuthAuthenticator;
    private final LocalAuthManager localAuthManager;
    private final MembersTermsAgreementReader membersTermsAgreementReader;
    private final MemberWithdrawalManager memberWithdrawalManager;
    private final SafeZoneReader safeZoneReader;
    private final PhoneVerificationWriter phoneVerificationWriter;

    public List<ContactMemberResponse> findByPhones(List<String> phoneNumbers) {
        List<Member> members = memberReader.findByPhones(phoneNumbers);

        return members.stream()
                .map(ContactMemberResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(String memberKey) {
        Member member = memberReader.findByMemberKey(memberKey);
        MembersTermsAgreement termsAgreement = membersTermsAgreementReader.findByMemberKey(memberKey);
        List<SafeZoneInfo> safeZones = safeZoneReader.findAllByWardMemberKey(memberKey);
        return MyInfoResponse.of(member, termsAgreement, safeZones);
    }

    @Transactional
    public void updateMyInfo(String memberKey, String name, LocalDate birth, String currentPassword, String newPassword,
                             String profileAvatarCode) {
        memberWriter.updateProfile(memberKey, name, birth);

        if (profileAvatarCode != null) {
            memberWriter.updateProfileAvatarCode(memberKey, ProfileAvatarCode.from(profileAvatarCode));
        }

        if (StringUtils.hasText(newPassword)) {
            localAuthAuthenticator.verifyPassword(memberKey, new Password(currentPassword));
            EncodedPassword encodedPassword = localAuthAuthenticator.encodePassword(new Password(newPassword));
            localAuthManager.changePassword(memberKey, encodedPassword.value());
        }
    }

    public void changePhone(String memberKey, String smsToken, Password password) {
        localAuthAuthenticator.verifyPassword(memberKey, password);
        PhoneNumber newPhone = phoneVerificationWriter.consumePhoneByToken(smsToken);
        memberWriter.changePhone(memberKey, newPhone);
    }

    public void withdraw(String memberKey, Password password) {
        memberWithdrawalManager.withdraw(memberKey, password);
    }
}
