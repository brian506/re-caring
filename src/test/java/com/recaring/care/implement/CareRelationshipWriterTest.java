package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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

    // ── delete ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("케어 관계 삭제 - 관계가 존재하면 entity.delete()가 호출된다")
    void delete_success() {
        CareRelationship relationship = CareFixture.createGuardianRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        given(careRelationshipRepository.findByWardKeyAndCaregiverKey(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(Optional.of(relationship));

        assertThatCode(() ->
                careRelationshipWriter.delete(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .doesNotThrowAnyException();

        then(careRelationshipRepository).should(times(1))
                .findByWardKeyAndCaregiverKey(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
    }

    @Test
    @DisplayName("케어 관계 삭제 - 관계가 존재하지 않으면 예외가 발생한다")
    void delete_fails_when_relationship_not_found() {
        given(careRelationshipRepository.findByWardKeyAndCaregiverKey(
                CareFixture.WARD_MEMBER_KEY, "unknown-key"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                careRelationshipWriter.delete(CareFixture.WARD_MEMBER_KEY, "unknown-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
    }
}
