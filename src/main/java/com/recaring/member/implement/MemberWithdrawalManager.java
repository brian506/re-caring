package com.recaring.member.implement;

import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.auth.vo.Password;
import com.recaring.care.implement.CareInvitationWriter;
import com.recaring.care.implement.CareRelationshipWriter;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.implement.GpsHistoryWriter;
import com.recaring.location.implement.GpsLatestCacheWriter;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberWithdrawal;
import com.recaring.member.dataaccess.repository.MemberWithdrawalRepository;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.safezone.implement.SafeZoneWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberWithdrawalManager {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final LocalAuthReader localAuthReader;
    private final LocalAuthAuthenticator localAuthAuthenticator;
    private final MemberWithdrawalRepository memberWithdrawalRepository;

    // 기존 Writer가 존재하는 도메인
    private final RefreshTokenWriter refreshTokenWriter;
    private final MembersTermsAgreementWriter membersTermsAgreementWriter;
    private final CareRelationshipWriter careRelationshipWriter;
    private final CareInvitationWriter careInvitationWriter;
    private final GpsHistoryWriter gpsHistoryWriter;
    private final SafeZoneWriter safeZoneWriter;
    private final GpsLatestCacheWriter gpsLatestCacheWriter;

    // Writer가 없는 도메인은 Repository를 직접 호출
    private final LocalAuthRepository localAuthRepository;
    private final OAuthRepository oAuthRepository;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final LocationSettingRepository locationSettingRepository;
    private final WardDeviceTokenRepository wardDeviceTokenRepository;

    @Transactional
    public void withdraw(String memberKey, Password password) {
        localAuthAuthenticator.verifyPassword(memberKey, password);

        Member member = memberReader.findByMemberKey(memberKey);
        String email = localAuthReader.findByMemberKey(memberKey).getEmail();

        memberWithdrawalRepository.save(MemberWithdrawal.of(memberKey, email, member.getRole()));

        refreshTokenWriter.deleteByMemberKey(memberKey);
        localAuthRepository.deleteByMemberKey(memberKey);
        oAuthRepository.deleteByMemberKey(memberKey);
        membersTermsAgreementWriter.deleteByMemberKey(memberKey);
        fcmDeviceTokenRepository.deleteByMemberKey(memberKey);
        notificationSettingRepository.deleteByWardMemberKey(memberKey);
        careRelationshipWriter.deleteAllByMemberKey(memberKey);
        careInvitationWriter.deleteAllByMemberKey(memberKey);
        gpsHistoryWriter.deleteByWardMemberKey(memberKey);
        locationSettingRepository.deleteByWardMemberKey(memberKey);
        safeZoneWriter.deleteByWardMemberKey(memberKey);
        wardDeviceTokenRepository.deleteByWardKey(memberKey);

        memberWriter.deleteByMemberKey(memberKey);
        gpsLatestCacheWriter.delete(memberKey);

        log.info("[회원 탈퇴 : 완료]: memberKey={}", memberKey);
    }
}
