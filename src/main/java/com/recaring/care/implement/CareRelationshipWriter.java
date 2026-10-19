package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CareRelationshipWriter {

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberReader memberReader;
    private final CareRelationshipValidator relationshipValidator;

    /**
     * 보호자 쪽 대상자 한도와 대상자 쪽 인원 한도를 함께 보므로 두 회원을 모두 잠근다.
     * 수락자가 대상자든 보호자든 같은 순서로 잠가야 교차 수락에서 교착이 생기지 않는다.
     */
    @CacheEvict(value = "careRelationship", allEntries = true)
    @Transactional
    public void register(CareRelationshipRegistration registration) {
        Stream.of(registration.wardMemberKey(), registration.caregiverKey())
                .sorted()
                .forEach(memberReader::lockForUpdate);
        relationshipValidator.validateCanAddWard(registration.caregiverKey(), registration.wardMemberKey());
        careRelationshipRepository.save(
                CareRelationship.of(registration.wardMemberKey(), registration.caregiverKey(), registration.careRole())
        );
    }

    @Transactional
    public void updateWardNickname(String wardKey, String caregiverKey, String wardNickname) {
        findRelationship(wardKey, caregiverKey).changeWardNickname(wardNickname);
    }

    private CareRelationship findRelationship(String wardKey, String caregiverKey) {
        return careRelationshipRepository
                .findCareRelationship(wardKey, caregiverKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));
    }
}
