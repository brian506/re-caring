package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
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
    @DisplayName("보호자가 등록하면 보호 대상자 추가 한도를 검증한 뒤 케어 관계를 저장한다")
    void register_validates_ward_limit_when_registrant_is_guardian() {
        // given
        Member guardian = CareFixture.createGuardianMember();
        CareRelationshipRegistration registration = CareFixture.createRegistration(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);
        given(memberReader.findForUpdate(CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(guardian);

        // when
        careRelationshipWriter.register(registration, CareFixture.GUARDIAN_MEMBER_KEY);

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
    @DisplayName("보호 대상자 본인이 등록하면 추가 한도를 검증하지 않고 케어 관계를 저장한다")
    void register_skips_ward_limit_when_registrant_is_ward() {
        // given
        Member ward = CareFixture.createWardMember();
        CareRelationshipRegistration registration = CareFixture.createRegistration(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);
        given(memberReader.findForUpdate(CareFixture.WARD_MEMBER_KEY)).willReturn(ward);

        // when
        careRelationshipWriter.register(registration, CareFixture.WARD_MEMBER_KEY);

        // then
        then(relationshipValidator).should(times(0)).validateCanAddWard(any(), any());
        then(careRelationshipRepository).should(times(1)).save(relationshipCaptor.capture());
        assertThat(relationshipCaptor.getValue())
                .extracting(
                        CareRelationship::getWardMemberKey,
                        CareRelationship::getCaregiverMemberKey,
                        CareRelationship::getCareRole)
                .containsExactly(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);
    }

    @Test
    @DisplayName("보호 대상자 추가 한도를 초과하면 케어 관계가 저장되지 않는다")
    void register_does_not_save_when_ward_limit_exceeded() {
        // given
        Member guardian = CareFixture.createGuardianMember();
        CareRelationshipRegistration registration = CareFixture.createRegistration(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN);
        given(memberReader.findForUpdate(CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(guardian);
        willThrow(new AppException(ErrorType.CARE_WARD_LIMIT_EXCEEDED))
                .given(relationshipValidator)
                .validateCanAddWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        // when / then
        assertThatThrownBy(() -> careRelationshipWriter.register(registration, CareFixture.GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_WARD_LIMIT_EXCEEDED);

        then(careRelationshipRepository).should(times(0)).save(any());
    }

    @Test
    @DisplayName("케어 관계 삭제 - 관계가 존재하면 repository.delete()가 호출된다")
    void delete_success() {
        // given
        CareRelationship relationship = CareFixture.createGuardianRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        given(careRelationshipRepository.findCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(Optional.of(relationship));

        // when / then
        assertThatCode(() ->
                careRelationshipWriter.delete(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .doesNotThrowAnyException();

        then(careRelationshipRepository).should(times(1))
                .findCareRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        then(careRelationshipRepository).should(times(1)).delete(relationship);
    }

    @Test
    @DisplayName("케어 관계 삭제 - 관계가 존재하지 않으면 예외가 발생한다")
    void delete_fails_when_relationship_not_found() {
        // given
        given(careRelationshipRepository.findCareRelationship(
                CareFixture.WARD_MEMBER_KEY, "unknown-key"))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
                careRelationshipWriter.delete(CareFixture.WARD_MEMBER_KEY, "unknown-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
    }
}
