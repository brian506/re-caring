package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import com.recaring.location.implement.setting.LocationSettingManager;
import com.recaring.notification.implement.setting.NotificationSettingManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareRelationshipManager {

    private final CareRelationshipRepository careRelationshipRepository;
    private final CareInvitationWriter careInvitationWriter;
    private final DesignatedAvatarManager designatedAvatarManager;
    private final NotificationSettingManager notificationSettingManager;
    private final LocationSettingManager locationSettingManager;

    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void leaveCare(String wardKey, String caregiverKey) {
        CareRelationship leaving = careRelationshipRepository.findCareRelationship(wardKey, caregiverKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));

        careRelationshipRepository.delete(leaving);
        designatedAvatarManager.deleteAllByRelationship(wardKey, caregiverKey);
        notificationSettingManager.deleteByRelationship(wardKey, caregiverKey);
        locationSettingManager.deleteByRelationship(wardKey, caregiverKey);
        log.info("[케어 관계 : 이탈]: wardKey={} | caregiverKey={} | careRole={}",
                wardKey, caregiverKey, leaving.getCareRole());

        careInvitationWriter.deletePendingSentBy(wardKey, caregiverKey);
    }

    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void leaveAllCare(String memberKey) {
        careRelationshipRepository.deleteAllByMemberKey(memberKey);
        designatedAvatarManager.deleteAllByMemberKey(memberKey);
    }
}
