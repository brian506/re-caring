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

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareRelationshipManager {

    /**
     * 승계 순서. 보호자를 관계자보다 먼저 올리고, 같은 역할 안에서는 먼저 등록된 쪽이 이긴다.
     */
    private static final Comparator<CareRelationship> SUCCESSION_ORDER =
            Comparator.comparingInt((CareRelationship relationship) ->
                            relationship.getCareRole() == CareRole.GUARDIAN ? 0 : 1)
                    .thenComparing(CareRelationship::getId);

    private final CareRelationshipRepository careRelationshipRepository;
    private final CareInvitationWriter careInvitationWriter;

    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void leaveCare(String wardKey, String caregiverKey) {
        List<CareRelationship> relationships = careRelationshipRepository.findAllByWardMemberKey(wardKey);
        CareRelationship leaving = relationships.stream()
                .filter(relationship -> relationship.getCaregiverMemberKey().equals(caregiverKey))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));

        careRelationshipRepository.delete(leaving);
        log.info("[케어 관계 : 이탈]: wardKey={} | caregiverKey={} | careRole={}",
                wardKey, caregiverKey, leaving.getCareRole());

        // 방금 지운 행을 메모리에서 걷어낸다. 다시 조회하면 삭제 flush 시점에 기대게 된다.
        settleAfterLeave(wardKey, relationships.stream()
                .filter(relationship -> !relationship.getCaregiverMemberKey().equals(caregiverKey))
                .toList());
    }

    /**
     * 탈퇴 처리. 떠나는 회원이 주보호자였던 대상자마다 승계가 필요하므로 벌크 삭제만으로 끝낼 수 없다.
     */
    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void leaveAllCare(String memberKey) {
        List<String> caredWardKeys = careRelationshipRepository.findAllByCaregiverMemberKey(memberKey).stream()
                .map(CareRelationship::getWardMemberKey)
                .toList();

        careRelationshipRepository.deleteAllByMemberKey(memberKey);

        for (String wardKey : caredWardKeys) {
            settleAfterLeave(wardKey, careRelationshipRepository.findAllByWardMemberKey(wardKey));
        }
    }

    /**
     * 주보호자가 0명인 대상자를 남기지 않는다. 그런 대상자는 보호자를 부를 주체도, 관계를 끊을 주체도 없다.
     * 남은 사람이 아무도 없으면 아직 수락되지 않은 초대까지 정리한다. 초대를 남겨두면 뒤늦은 수락으로
     * 관계가 되살아나고, 초대를 보낸 주보호자는 이미 떠난 뒤라 아무도 그 관계를 책임지지 않는다.
     */
    private void settleAfterLeave(String wardKey, List<CareRelationship> remaining) {
        if (remaining.isEmpty()) {
            careInvitationWriter.deleteAllByWardMemberKey(wardKey);
            log.info("[케어 관계 : 마지막 관계 해제로 초대 정리]: wardKey={}", wardKey);
            return;
        }

        boolean hasPrimaryGuardian = remaining.stream()
                .anyMatch(relationship -> relationship.getCareRole() == CareRole.PRIMARY_GUARDIAN);
        if (hasPrimaryGuardian) {
            return;
        }

        CareRelationship successor = remaining.stream()
                .min(SUCCESSION_ORDER)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));
        CareRole previousRole = successor.getCareRole();
        successor.changeCareRole(CareRole.PRIMARY_GUARDIAN);

        log.info("[케어 관계 : 주보호자 승계]: wardKey={} | caregiverKey={} | previousRole={}",
                wardKey, successor.getCaregiverMemberKey(), previousRole);
    }
}
