package com.recaring.safezone;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.safezone.business.SafeZoneService;
import com.recaring.safezone.controller.request.CreateSafeZoneCommand;
import com.recaring.safezone.controller.request.UpdateSafeZoneCommand;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.implement.SafeZoneWriter;
import com.recaring.safezone.vo.SafeZoneInfo;
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
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneService 단위 테스트")
class SafeZoneServiceTest {

    @InjectMocks
    private SafeZoneService safeZoneService;

    @Mock
    private SafeZoneReader safeZoneReader;

    @Mock
    private SafeZoneWriter safeZoneWriter;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    // ── addSafeZone ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("CareRole.GUARDIAN인 보호자는 안심존을 추가한다")
    void addSafeZone_success_when_guardian() {
        CreateSafeZoneCommand command = SafeZoneFixture.createCommand();
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(true);

        safeZoneService.addSafeZone(CareFixture.GUARDIAN_MEMBER_KEY, command);

        then(safeZoneWriter).should(times(1)).register(command);
    }

    @Test
    @DisplayName("CareRole.MANAGER인 관계자가 안심존 추가 시 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void addSafeZone_throws_when_manager() {
        CreateSafeZoneCommand command = SafeZoneFixture.createCommand();
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(false);

        assertThatThrownBy(() -> safeZoneService.addSafeZone(CareFixture.MANAGER_MEMBER_KEY, command))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(times(0)).register(any());
    }

    @Test
    @DisplayName("케어 관계가 없으면 안심존 추가 시 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void addSafeZone_throws_when_not_guardian() {
        CreateSafeZoneCommand command = SafeZoneFixture.createCommand();
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(false);

        assertThatThrownBy(() -> safeZoneService.addSafeZone(CareFixture.GUARDIAN_MEMBER_KEY, command))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(times(0)).register(any());
    }

    // ── getSafeZones ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CareRole.GUARDIAN인 보호자는 안심존 목록을 조회한다")
    void getSafeZones_returns_list_when_guardian() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        List<SafeZoneInfo> expected = List.of(SafeZoneFixture.createSafeZoneInfo(zone.getSafeZoneKey()));
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKey(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(true);
        given(safeZoneReader.findAllByWardMemberKey(SafeZoneFixture.WARD_MEMBER_KEY)).willReturn(expected);

        List<SafeZoneInfo> result = safeZoneService.getSafeZones(CareFixture.GUARDIAN_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("CareRole.MANAGER인 관계자도 안심존 목록을 조회한다")
    void getSafeZones_returns_list_when_manager() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        List<SafeZoneInfo> expected = List.of(SafeZoneFixture.createSafeZoneInfo(zone.getSafeZoneKey()));
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKey(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)).willReturn(true);
        given(safeZoneReader.findAllByWardMemberKey(SafeZoneFixture.WARD_MEMBER_KEY)).willReturn(expected);

        List<SafeZoneInfo> result = safeZoneService.getSafeZones(CareFixture.MANAGER_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("케어 관계가 없으면 목록 조회 시 NOT_CAREGIVER_OF_WARD 예외가 발생한다")
    void getSafeZones_throws_when_not_caregiver() {
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKey(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(false);

        assertThatThrownBy(() -> safeZoneService.getSafeZones(CareFixture.GUARDIAN_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CAREGIVER_OF_WARD);

        then(safeZoneReader).should(times(0)).findAllByWardMemberKey(any());
    }

    // ── getSafeZone ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("CareRole.GUARDIAN인 보호자는 안심존 상세를 조회한다")
    void getSafeZone_returns_info_when_guardian() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        SafeZoneInfo expected = SafeZoneFixture.createSafeZoneInfo(zone.getSafeZoneKey());
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKey(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(true);
        given(safeZoneReader.findBySafeZoneKey(zone.getSafeZoneKey())).willReturn(expected);

        SafeZoneInfo result = safeZoneService.getSafeZone(
                CareFixture.GUARDIAN_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, zone.getSafeZoneKey());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("CareRole.MANAGER인 관계자도 안심존 상세를 조회한다")
    void getSafeZone_returns_info_when_manager() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        SafeZoneInfo expected = SafeZoneFixture.createSafeZoneInfo(zone.getSafeZoneKey());
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKey(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)).willReturn(true);
        given(safeZoneReader.findBySafeZoneKey(zone.getSafeZoneKey())).willReturn(expected);

        SafeZoneInfo result = safeZoneService.getSafeZone(
                CareFixture.MANAGER_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, zone.getSafeZoneKey());

        assertThat(result).isEqualTo(expected);
    }

    // ── updateSafeZone ───────────────────────────────────────────────────────

    @Test
    @DisplayName("CareRole.GUARDIAN인 보호자는 안심존을 수정한다")
    void updateSafeZone_success_when_guardian() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        UpdateSafeZoneCommand command = SafeZoneFixture.updateCommand();
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(true);
        given(safeZoneReader.getEntity(zone.getSafeZoneKey())).willReturn(zone);

        safeZoneService.updateSafeZone(CareFixture.GUARDIAN_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, zone.getSafeZoneKey(), command);

        then(safeZoneWriter).should(times(1)).update(zone, command);
    }

    @Test
    @DisplayName("CareRole.MANAGER인 관계자가 안심존 수정 시 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void updateSafeZone_throws_when_manager() {
        UpdateSafeZoneCommand command = SafeZoneFixture.updateCommand();
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(false);

        assertThatThrownBy(() -> safeZoneService.updateSafeZone(
                CareFixture.MANAGER_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, "any-key", command))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(times(0)).update(any(), any());
    }

    @Test
    @DisplayName("케어 관계가 없으면 수정 시 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void updateSafeZone_throws_when_not_guardian() {
        UpdateSafeZoneCommand command = SafeZoneFixture.updateCommand();
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(false);

        assertThatThrownBy(() -> safeZoneService.updateSafeZone(
                CareFixture.GUARDIAN_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, "any-key", command))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(times(0)).update(any(), any());
    }

    // ── deleteSafeZone ───────────────────────────────────────────────────────

    @Test
    @DisplayName("CareRole.GUARDIAN인 보호자는 안심존을 삭제한다")
    void deleteSafeZone_success_when_guardian() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(true);
        given(safeZoneReader.getEntity(zone.getSafeZoneKey())).willReturn(zone);

        safeZoneService.deleteSafeZone(CareFixture.GUARDIAN_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, zone.getSafeZoneKey());

        then(safeZoneWriter).should(times(1)).delete(zone);
    }

    @Test
    @DisplayName("CareRole.MANAGER인 관계자가 안심존 삭제 시 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void deleteSafeZone_throws_when_manager() {
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(false);

        assertThatThrownBy(() -> safeZoneService.deleteSafeZone(
                CareFixture.MANAGER_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, "any-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(times(0)).delete(any());
    }

    @Test
    @DisplayName("케어 관계가 없으면 삭제 시 NOT_GUARDIAN_OF_WARD 예외가 발생한다")
    void deleteSafeZone_throws_when_not_guardian() {
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                SafeZoneFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)).willReturn(false);

        assertThatThrownBy(() -> safeZoneService.deleteSafeZone(
                CareFixture.GUARDIAN_MEMBER_KEY, SafeZoneFixture.WARD_MEMBER_KEY, "any-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(safeZoneWriter).should(times(0)).delete(any());
    }
}
