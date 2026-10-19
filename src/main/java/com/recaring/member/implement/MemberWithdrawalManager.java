package com.recaring.member.implement;

import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.vo.Password;
import com.recaring.care.implement.CareInvitationWriter;
import com.recaring.care.implement.CareRelationshipManager;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.inquiry.implement.InquiryManager;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.implement.detection.AnomalyAcknowledgementManager;
import com.recaring.location.implement.detection.AnomalyDetectionManager;
import com.recaring.location.implement.detection.BatteryAlertStateManager;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import com.recaring.safezone.implement.SafeZoneStateManager;
import com.recaring.member.dataaccess.entity.MemberWithdrawal;
import com.recaring.member.vo.MemberRole;
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
    private final LocalAuthAuthenticator localAuthAuthenticator;
    private final MemberWithdrawalRepository memberWithdrawalRepository;

    // 기존 Writer가 존재하는 도메인
    private final RefreshTokenWriter refreshTokenWriter;
    private final MembersTermsAgreementWriter membersTermsAgreementWriter;
    private final CareRelationshipManager careRelationshipManager;
    private final CareInvitationWriter careInvitationWriter;
    private final GpsHistoryManager gpsHistoryManager;
    private final AnomalyDetectionManager anomalyDetectionManager;
    private final SafeZoneWriter safeZoneWriter;
    private final GpsLatestCacheManager gpsLatestCacheManager;
    private final BatteryAlertStateManager batteryAlertStateManager;
    private final AnomalyAcknowledgementManager anomalyAcknowledgementManager;
    private final SafeZoneStateManager safeZoneStateManager;
    private final InquiryManager inquiryManager;

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

        MemberRole role = memberReader.findByMemberKey(memberKey).role();

        memberWithdrawalRepository.save(MemberWithdrawal.of(memberKey, role));

        refreshTokenWriter.deleteByMemberKey(memberKey);
        localAuthRepository.deleteByMemberKey(memberKey);
        oAuthRepository.deleteByMemberKey(memberKey);
        membersTermsAgreementWriter.deleteByMemberKey(memberKey);
        fcmDeviceTokenRepository.deleteByMemberKey(memberKey);
        notificationSettingRepository.deleteByMemberKey(memberKey);
        careRelationshipManager.leaveAllCare(memberKey);
        careInvitationWriter.deleteAllByMemberKey(memberKey);
        gpsHistoryManager.deleteByWardMemberKey(memberKey);
        anomalyDetectionManager.deleteByWardMemberKey(memberKey);
        locationSettingRepository.deleteByMemberKey(memberKey);
        safeZoneWriter.deleteByWardMemberKey(memberKey);
        safeZoneStateManager.delete(memberKey);
        wardDeviceTokenRepository.deleteByWardKey(memberKey);
        inquiryManager.deleteByMemberKey(memberKey);

        memberWriter.deleteByMemberKey(memberKey);
        gpsLatestCacheManager.delete(memberKey);
        batteryAlertStateManager.delete(memberKey);
        anomalyAcknowledgementManager.delete(memberKey);

        log.info("[MemberWithdrawal : Completed]: memberKey={}", memberKey);
    }
}
