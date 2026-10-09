package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareRelationshipWriter 단위 테스트")
class CareRelationshipWriterTest {

    @InjectMocks
    private CareRelationshipWriter careRelationshipWriter;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private MemberReader memberReader;

    @Mock
    private CareRelationshipValidator relationshipValidator;

    @Captor
    private ArgumentCaptor<CareRelationship> relationshipCaptor;

    @Test
    @DisplayName("수락자와 무관하게 등록되는 보호자 기준으로 대상자 한도와 인원 한도를 검증한 뒤 케어 관계를 저장한다")
    void register_validates_limits_of_the_registered_caregiver_and_saves() {
        // given
        CareRelationshipRegistration registration = CareFixture.createRegistration(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);

        // when
        careRelationshipWriter.register(registration);

        // then
        then(relationshipValidator).should(times(1))
                .validateCanAddWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipRepository).should(times(1)).save(relationshipCaptor.capture());
        assertThat(relationshipCaptor.getValue())
                .extracting(
                        CareRelationship::getWardMemberKey,
                        CareRelationship::getCaregiverMemberKey,
                        CareRelationship::getCareRole)
                .containsExactly(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);
    }

    @Test
    @DisplayName("한도를 초과하면 케어 관계가 저장되지 않는다")
    void register_does_not_save_when_limit_exceeded() {
        // given
        CareRelationshipRegistration registration = CareFixture.createRegistration(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);
        willThrow(new AppException(ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED))
                .given(relationshipValidator)
                .validateCanAddWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        // when / then
        assertThatThrownBy(() -> careRelationshipWriter.register(registration))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);

        then(careRelationshipRepository).should(times(0)).save(any());
    }

    @Test
    @DisplayName("두 회원은 대상자·보호자 순서와 무관하게 memberKey 오름차순으로 잠근 뒤 한도를 검증한다")
    void register_locks_both_members_in_key_order_before_validation() {
        // given — "guardian-member-key" < "ward-member-key"
        CareRelationshipRegistration registration = CareFixture.createRegistration(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);

        // when
        careRelationshipWriter.register(registration);

        // then
        InOrder order = inOrder(memberReader, relationshipValidator);
        order.verify(memberReader).findForUpdate(CareFixture.GUARDIAN_MEMBER_KEY);
        order.verify(memberReader).findForUpdate(CareFixture.WARD_MEMBER_KEY);
        order.verify(relationshipValidator)
                .validateCanAddWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
    }
}
