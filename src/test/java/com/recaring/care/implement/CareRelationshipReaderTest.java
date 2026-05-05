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
@DisplayName("케어 관계 Reader 단위 테스트")
class CareRelationshipReaderTest {

    @InjectMocks
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("대상자에 대한 케어 관계 역할을 조회한다")
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
    @DisplayName("대상자와 관계가 없으면 예외가 발생한다")
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
}
