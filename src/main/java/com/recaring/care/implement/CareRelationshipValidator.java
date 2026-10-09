package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.implement.MemberValidator;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class CareRelationshipValidator {

    private static final int MAX_WARD_COUNT = 5;
    private static final int MAX_CAREGIVER_COUNT = 5;

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberValidator memberValidator;


    /**
     * 한도는 두 축이다.
     * - 보호자 1명이 맡을 수 있는 대상자: 최대 MAX_WARD_COUNT 명
     * - 대상자 1명에 붙을 수 있는 케어 관계: 역할과 무관하게 최대 MAX_CAREGIVER_COUNT 명
     * 대상자 추가 요청은 이미 보호자가 있는 대상자에게도 보낼 수 있으므로 두 축을 모두 본다.
     */
    public void validateCanAddWard(String caregiverMemberKey, String wardMemberKey) {
        memberValidator.validateSubscription(caregiverMemberKey);

        List<CareRelationship> caregiverRelationships = careRelationshipRepository.findAllByCaregiverMemberKey(caregiverMemberKey);
        checkGuardianLimit(caregiverRelationships);
        validateNotDuplicated(caregiverRelationships, CareRelationship::getWardMemberKey, wardMemberKey);
        checkCaregiverLimit(careRelationshipRepository.findAllByWardMemberKey(wardMemberKey));
    }

    public void validateCanAddCaregiver(String requesterKey, String wardMemberKey, String newCaregiverKey) {
        validateGuardianRole(requesterKey, wardMemberKey);

        // memberValidator.validatePremium(requesterKey);
        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByWardMemberKey(wardMemberKey);
        checkCaregiverLimit(careRelationships);
        validateNotDuplicated(careRelationships, CareRelationship::getCaregiverMemberKey, newCaregiverKey);
    }

    public void validateCaregiverViewAccess(String requesterKey, String wardKey) {
        boolean isWardSelf = wardKey.equals(requesterKey);
        boolean isGuardian = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey, CareRole.GUARDIAN);
        if (!isWardSelf && !isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }
    }

    public void validateCaregiver(String requesterKey, String wardKey) {
        boolean isCaregiver = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey);
        if (!isCaregiver) {
            throw new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
        }
    }

    private void validateGuardianRole(String requesterKey, String wardKey) {
        boolean isGuardian = careRelationshipRepository.existsCareRelationship(wardKey, requesterKey, CareRole.GUARDIAN);
        if (!isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }
    }

    private void checkCaregiverLimit(List<CareRelationship> relationships) {
        if (relationships.size() >= MAX_CAREGIVER_COUNT) {
            throw new AppException(ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
        }
    }

    private void checkGuardianLimit(List<CareRelationship> relationships) {
        long count = relationships.stream()
                .filter(r -> r.getCareRole().isGuardian())
                .count();
        if (count >= MAX_WARD_COUNT) {
            throw new AppException(ErrorType.CARE_WARD_LIMIT_EXCEEDED);
        }
    }

    private void validateNotDuplicated(List<CareRelationship> careRelationships, Function<CareRelationship, String> key, String targetKey) {
        boolean isDuplicated = careRelationships.stream()
                .map(key)
                .anyMatch(targetKey::equals);

        if (isDuplicated) {
            throw new AppException(ErrorType.ALREADY_CARE_RELATIONSHIP);
        }
    }
}
