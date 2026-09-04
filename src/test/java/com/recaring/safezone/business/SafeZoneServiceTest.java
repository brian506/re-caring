package com.recaring.safezone.business;

import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.implement.SafeZoneWriter;
import com.recaring.safezone.vo.SafeZoneCreation;
import com.recaring.safezone.vo.SafeZoneUpdate;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneService 단위 테스트")
class SafeZoneServiceTest {

    private static final String WARD_KEY = SafeZoneFixture.WARD_MEMBER_KEY;
    private static final String REQUESTER_KEY = CareFixture.GUARDIAN_MEMBER_KEY;
    private static final String SAFE_ZONE_KEY = SafeZoneFixture.SAFE_ZONE_KEY;

    @InjectMocks
    private SafeZoneService safeZoneService;

    @Mock
    private SafeZoneReader safeZoneReader;

    @Mock
    private SafeZoneWriter safeZoneWriter;

    @Mock
    private CareRelationshipReader careRelationshipReader;

    // ── addSafeZone ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("보호자 관계가 확인되면 안심존 등록을 위임한다")
    void addSafeZone_delegates_when_guardian() {
        SafeZoneCreation command = SafeZoneFixture.createCommand();
        givenGuardianOfWard(true);

        safeZoneService.addSafeZone(REQUESTER_KEY, command);

        then(safeZoneWriter).should().register(command);
    }

    @Test
    @DisplayName("보호자 관계가 아니면 안심존을 등록하지 않고 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void addSafeZone_throws_when_not_guardian() {
        SafeZoneCreation command = SafeZoneFixture.createCommand();
        givenGuardianOfWard(false);

        assertThatThrownBy(() -> safeZoneService.addSafeZone(REQUESTER_KEY, command))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(never()).register(any());
    }

    // ── getSafeZones ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("케어 관계가 확인되면 해당 보호 대상자의 안심존 목록 조회를 위임한다")
    void getSafeZones_delegates_when_caregiver() {
        givenCaregiverOfWard(true);

        safeZoneService.getSafeZones(REQUESTER_KEY, WARD_KEY);

        then(safeZoneReader).should().findAllByWardMemberKey(WARD_KEY);
    }

    @Test
    @DisplayName("케어 관계가 없으면 목록을 조회하지 않고 NOT_CAREGIVER_OF_WARD 예외가 발생한다")
    void getSafeZones_throws_when_not_caregiver() {
        givenCaregiverOfWard(false);

        assertThatThrownBy(() -> safeZoneService.getSafeZones(REQUESTER_KEY, WARD_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CAREGIVER_OF_WARD);

        then(safeZoneReader).should(never()).findAllByWardMemberKey(any());
    }

    // ── getSafeZone ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("케어 관계가 확인되면 안심존 상세 조회를 위임한다")
    void getSafeZone_delegates_when_caregiver() {
        givenCaregiverOfWard(true);

        safeZoneService.getSafeZone(REQUESTER_KEY, WARD_KEY, SAFE_ZONE_KEY);

        then(safeZoneReader).should().findBySafeZoneKey(SAFE_ZONE_KEY, WARD_KEY);
    }

    @Test
    @DisplayName("케어 관계가 없으면 상세를 조회하지 않고 NOT_CAREGIVER_OF_WARD 예외가 발생한다")
    void getSafeZone_throws_when_not_caregiver() {
        givenCaregiverOfWard(false);

        assertThatThrownBy(() -> safeZoneService.getSafeZone(REQUESTER_KEY, WARD_KEY, SAFE_ZONE_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CAREGIVER_OF_WARD);

        then(safeZoneReader).should(never()).findBySafeZoneKey(any(), any());
    }

    // ── updateSafeZone ───────────────────────────────────────────────────────

    @Test
    @DisplayName("보호자 관계가 확인되면 안심존 수정을 위임한다")
    void updateSafeZone_delegates_when_guardian() {
        SafeZoneUpdate command = SafeZoneFixture.updateCommand();
        givenGuardianOfWard(true);

        safeZoneService.updateSafeZone(REQUESTER_KEY, WARD_KEY, SAFE_ZONE_KEY, command);

        then(safeZoneWriter).should().update(SAFE_ZONE_KEY, WARD_KEY, command);
    }

    @Test
    @DisplayName("보호자 관계가 아니면 안심존을 수정하지 않고 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void updateSafeZone_throws_when_not_guardian() {
        SafeZoneUpdate command = SafeZoneFixture.updateCommand();
        givenGuardianOfWard(false);

        assertThatThrownBy(() -> safeZoneService.updateSafeZone(REQUESTER_KEY, WARD_KEY, SAFE_ZONE_KEY, command))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(never()).update(any(), any(), any());
    }

    // ── deleteSafeZone ───────────────────────────────────────────────────────

    @Test
    @DisplayName("보호자 관계가 확인되면 안심존 삭제를 위임한다")
    void deleteSafeZone_delegates_when_guardian() {
        givenGuardianOfWard(true);

        safeZoneService.deleteSafeZone(REQUESTER_KEY, WARD_KEY, SAFE_ZONE_KEY);

        then(safeZoneWriter).should().delete(SAFE_ZONE_KEY, WARD_KEY);
    }

    @Test
    @DisplayName("보호자 관계가 아니면 안심존을 삭제하지 않고 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void deleteSafeZone_throws_when_not_guardian() {
        givenGuardianOfWard(false);

        assertThatThrownBy(() -> safeZoneService.deleteSafeZone(REQUESTER_KEY, WARD_KEY, SAFE_ZONE_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(never()).delete(any(), any());
    }

    private void givenGuardianOfWard(boolean result) {
        given(careRelationshipReader.existsWithGuardianRole(WARD_KEY, REQUESTER_KEY)).willReturn(result);
    }

    private void givenCaregiverOfWard(boolean result) {
        given(careRelationshipReader.exists(WARD_KEY, REQUESTER_KEY)).willReturn(result);
    }
}
