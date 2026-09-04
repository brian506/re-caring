package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
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

    /**
     * 주보호자가 떠나면 남은 보호자·관계자를 정리할 주체가 사라진다. 그래서 주보호자의 이탈은
     * 그 대상자의 케어 관계와 아직 수락되지 않은 초대까지 함께 끝낸다. 초대를 남겨두면 뒤늦은 수락으로
     * 주보호자 없는 대상자에 관계가 되살아나고, 그때는 아무도 그 관계를 끊을 수 없다.
     */
    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void leaveCare(String wardKey, String caregiverKey) {
        CareRelationship relationship = careRelationshipRepository
                .findCareRelationship(wardKey, caregiverKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));

        if (relationship.getCareRole() != CareRole.PRIMARY_GUARDIAN) {
            careRelationshipRepository.delete(relationship);
            log.info("[케어 관계 : 이탈]: wardKey={} | caregiverKey={} | careRole={}",
                    wardKey, caregiverKey, relationship.getCareRole());
            return;
        }

        careRelationshipRepository.deleteAllByWardMemberKey(wardKey);
        careInvitationWriter.deleteAllByWardMemberKey(wardKey);
        log.info("[케어 관계 : 주보호자 이탈로 일괄 해제]: wardKey={} | primaryGuardianKey={}", wardKey, caregiverKey);
    }
}
