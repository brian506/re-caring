package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareRelationshipReader unit test")
class CareRelationshipReaderTest {

    @InjectMocks
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("Returns the care role between a ward and caregiver")
    void findCareRole_returns_role() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)
                ));

        CareRole result = careRelationshipReader.findCareRole(
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY
        );

        assertThat(result).isEqualTo(CareRole.MANAGER);
        then(careRelationshipRepository).should(times(1)).findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY);
    }

    @Test
    @DisplayName("Throws when the ward and caregiver are unrelated")
    void findCareRole_throws_exception_when_not_related() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)
                ));

        assertThatThrownBy(() -> careRelationshipReader.findCareRole(
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CARE_RELATED_WARD);
    }

    @Test
    @DisplayName("Checks whether caregiver has the requested care role")
    void existsWithCareRole_returns_relationship_existence() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY,
                CareRole.MANAGER
        )).willReturn(true);

        boolean result = careRelationshipReader.existsWithCareRole(
                CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY,
                CareRole.MANAGER
        );

        assertThat(result).isTrue();
    }
}
