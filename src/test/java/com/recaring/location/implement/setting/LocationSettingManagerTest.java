package com.recaring.location.implement.setting;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.LocationCollectionInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("위치 수집 주기 설정 단위 테스트")
class LocationSettingManagerTest {

    private static final String GUARDIAN_KEY = LocationFixture.GUARDIAN_KEY;
    private static final String OTHER_GUARDIAN_KEY = LocationFixture.OTHER_GUARDIAN_KEY;
    private static final String MANAGER_KEY = LocationFixture.MANAGER_KEY;

    @InjectMocks
    private LocationSettingManager locationSettingManager;

    @Mock
    private LocationSettingRepository locationSettingRepository;

    @Mock
    private CareRelationshipReader careRelationshipReader;

    @Test
    @DisplayName("그 보호자의 설정이 없으면 그 보호자 몫의 설정을 새로 만들어 저장한다")
    void updateCollectionInterval_creates_setting_of_the_caregiver_when_absent() {
        given(locationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(LocationFixture.WARD_KEY, GUARDIAN_KEY))
                .willReturn(Optional.empty());

        locationSettingManager.updateCollectionInterval(
                LocationFixture.WARD_KEY, GUARDIAN_KEY, LocationCollectionInterval.THREE_MINUTES);

        ArgumentCaptor<LocationSetting> captor = ArgumentCaptor.forClass(LocationSetting.class);
        then(locationSettingRepository).should().save(captor.capture());
        assertThat(captor.getValue().getWardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(captor.getValue().getCaregiverMemberKey()).isEqualTo(GUARDIAN_KEY);
        assertThat(captor.getValue().getCollectionIntervalSeconds()).isEqualTo(180);
    }

    @Test
    @DisplayName("그 보호자의 설정이 있으면 그 설정의 주기를 수정한다")
    void updateCollectionInterval_updates_existing_setting_of_the_caregiver() {
        LocationSetting setting = LocationFixture.createLocationSetting(GUARDIAN_KEY, 30);
        given(locationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(LocationFixture.WARD_KEY, GUARDIAN_KEY))
                .willReturn(Optional.of(setting));

        locationSettingManager.updateCollectionInterval(
                LocationFixture.WARD_KEY, GUARDIAN_KEY, LocationCollectionInterval.ONE_MINUTE);

        then(locationSettingRepository).should().save(setting);
        assertThat(setting.getCollectionIntervalSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("그 보호자의 설정이 없으면 30초를 돌려준다")
    void findCollectionInterval_returns_thirty_seconds_when_absent() {
        given(locationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(LocationFixture.WARD_KEY, GUARDIAN_KEY))
                .willReturn(Optional.empty());

        LocationCollectionInterval result =
                locationSettingManager.findCollectionInterval(LocationFixture.WARD_KEY, GUARDIAN_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
    }

    @Test
    @DisplayName("그 보호자의 설정이 있으면 그 주기를 돌려준다")
    void findCollectionInterval_returns_saved_interval_of_the_caregiver() {
        given(locationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(LocationFixture.WARD_KEY, GUARDIAN_KEY))
                .willReturn(Optional.of(LocationFixture.createLocationSetting(GUARDIAN_KEY, 300)));

        LocationCollectionInterval result =
                locationSettingManager.findCollectionInterval(LocationFixture.WARD_KEY, GUARDIAN_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.FIVE_MINUTES);
    }

    @Test
    @DisplayName("기기 수집 주기는 보호자 계열 설정 중 가장 짧은 주기다")
    void findDeviceCollectionInterval_returns_shortest_among_guardians() {
        given(locationSettingRepository.findAllByWardMemberKey(LocationFixture.WARD_KEY)).willReturn(List.of(
                LocationFixture.createLocationSetting(GUARDIAN_KEY, 300),
                LocationFixture.createLocationSetting(OTHER_GUARDIAN_KEY, 60)));
        given(careRelationshipReader.findCaregiverInfos(LocationFixture.WARD_KEY)).willReturn(List.of(
                CareFixture.createCaregiverInfo(GUARDIAN_KEY, CareRole.GUARDIAN),
                CareFixture.createCaregiverInfo(OTHER_GUARDIAN_KEY, CareRole.GUARDIAN)));

        LocationCollectionInterval result = locationSettingManager.findDeviceCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.ONE_MINUTE);
    }

    @Test
    @DisplayName("설정하지 않은 보호자가 한 명이라도 있으면 기기 수집 주기는 30초다")
    void findDeviceCollectionInterval_returns_thirty_seconds_when_a_guardian_never_configured() {
        given(locationSettingRepository.findAllByWardMemberKey(LocationFixture.WARD_KEY)).willReturn(List.of(
                LocationFixture.createLocationSetting(GUARDIAN_KEY, 300)));
        given(careRelationshipReader.findCaregiverInfos(LocationFixture.WARD_KEY)).willReturn(List.of(
                CareFixture.createCaregiverInfo(GUARDIAN_KEY, CareRole.GUARDIAN),
                CareFixture.createCaregiverInfo(OTHER_GUARDIAN_KEY, CareRole.GUARDIAN)));

        LocationCollectionInterval result = locationSettingManager.findDeviceCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
    }

    @Test
    @DisplayName("관계자는 주기를 고르지 않으므로 기기 수집 주기 계산에서 빠진다")
    void findDeviceCollectionInterval_ignores_manager() {
        given(locationSettingRepository.findAllByWardMemberKey(LocationFixture.WARD_KEY)).willReturn(List.of(
                LocationFixture.createLocationSetting(GUARDIAN_KEY, 300)));
        given(careRelationshipReader.findCaregiverInfos(LocationFixture.WARD_KEY)).willReturn(List.of(
                CareFixture.createCaregiverInfo(GUARDIAN_KEY, CareRole.GUARDIAN),
                CareFixture.createCaregiverInfo(MANAGER_KEY, CareRole.MANAGER)));

        LocationCollectionInterval result = locationSettingManager.findDeviceCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.FIVE_MINUTES);
    }

    @Test
    @DisplayName("케어 관계가 끊긴 보호자의 설정은 기기 수집 주기 계산에 들어가지 않는다")
    void findDeviceCollectionInterval_ignores_setting_of_former_caregiver() {
        given(locationSettingRepository.findAllByWardMemberKey(LocationFixture.WARD_KEY)).willReturn(List.of(
                LocationFixture.createLocationSetting(GUARDIAN_KEY, 300),
                LocationFixture.createLocationSetting(OTHER_GUARDIAN_KEY, 30)));
        given(careRelationshipReader.findCaregiverInfos(LocationFixture.WARD_KEY)).willReturn(List.of(
                CareFixture.createCaregiverInfo(GUARDIAN_KEY, CareRole.GUARDIAN)));

        LocationCollectionInterval result = locationSettingManager.findDeviceCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.FIVE_MINUTES);
    }

    @Test
    @DisplayName("케어 관계가 하나도 없으면 기기 수집 주기는 30초다")
    void findDeviceCollectionInterval_returns_thirty_seconds_without_caregiver() {
        given(locationSettingRepository.findAllByWardMemberKey(LocationFixture.WARD_KEY)).willReturn(List.of());
        given(careRelationshipReader.findCaregiverInfos(LocationFixture.WARD_KEY)).willReturn(List.of());

        LocationCollectionInterval result = locationSettingManager.findDeviceCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
    }
}
