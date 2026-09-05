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
        settleAfterLeave(wardKey, caregiverKey, relationships.stream()
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

        // 벌크 삭제된 행은 DB에서 사라져 아래 재조회에 잡히지 않고, 영속성 컨텍스트에 남은 행은
        // 수정하지 않으므로 커밋 시 되살아나지 않는다.
        careRelationshipRepository.deleteAllByMemberKey(memberKey);

        for (String wardKey : caredWardKeys) {
            settleAfterLeave(wardKey, memberKey, careRelationshipRepository.findAllByWardMemberKey(wardKey));
        }
    }

    /**
     * 주보호자가 0명인 대상자를 남기지 않는다. 그런 대상자는 보호자를 부를 주체도, 관계를 끊을 주체도 없다.
     *
     * 떠나는 사람이 보낸 초대는 함께 지운다. 초대에는 만료가 없고 수락 시점에 발신자의 권한을 다시 보지 않으므로,
     * 남겨두면 이탈 후 뒤늦은 수락으로 관계가 생긴다. 그 관계는 승계된 주보호자가 승인한 적 없는 것이다.
     * 남은 사람이 아무도 없을 때는 발신자를 가리지 않고 그 대상자의 초대를 모두 정리한다.
     */
    private void settleAfterLeave(String wardKey, String leavingCaregiverKey, List<CareRelationship> remaining) {
        careInvitationWriter.deletePendingSentBy(wardKey, leavingCaregiverKey);

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
