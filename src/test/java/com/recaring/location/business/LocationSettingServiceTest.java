package com.recaring.location.business;

import com.recaring.location.business.command.UpdateLocationCollectionIntervalCommand;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.LocationSettingManager;
import com.recaring.location.implement.LocationSettingReader;
import com.recaring.location.implement.LocationValidator;
import com.recaring.location.vo.LocationCollectionInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationSettingService 단위 테스트")
class LocationSettingServiceTest {

    @InjectMocks
    private LocationSettingService locationSettingService;

    @Mock
    private LocationSettingReader locationSettingReader;
    @Mock
    private LocationSettingManager locationSettingManager;
    @Mock
    private LocationValidator locationValidator;

    @Test
    @DisplayName("주보호자는 현재 위치 수집 주기와 옵션을 조회한다")
    void getCollectionInterval_returns_setting_info_for_guardian() {
        given(locationSettingReader.findCollectionInterval(LocationFixture.WARD_KEY))
                .willReturn(LocationCollectionInterval.DEFAULT);

        LocationCollectionIntervalSettingInfo result = locationSettingService.getCollectionInterval(
                LocationFixture.GUARDIAN_KEY,
                LocationFixture.WARD_KEY
        );

        then(locationValidator).should().validateGuardianAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        assertThat(result.currentIntervalSeconds()).isEqualTo(5);
        assertThat(result.defaultIntervalSeconds()).isEqualTo(5);
        assertThat(result.options()).containsExactly(5, 10, 30, 60, 180, 300);
    }

    @Test
    @DisplayName("주보호자는 위치 수집 주기를 수정한다")
    void updateCollectionInterval_updates_setting_for_guardian() {
        UpdateLocationCollectionIntervalCommand command = new UpdateLocationCollectionIntervalCommand(
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THIRTY_SECONDS
        );

        locationSettingService.updateCollectionInterval(LocationFixture.GUARDIAN_KEY, command);

        then(locationValidator).should().validateGuardianAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        then(locationSettingManager).should().updateCollectionInterval(
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THIRTY_SECONDS
        );
    }

    @Test
    @DisplayName("보호 대상자는 자신의 현재 위치 수집 주기만 조회한다")
    void getMyCollectionInterval_returns_current_interval_only() {
        given(locationSettingReader.findCollectionInterval(LocationFixture.WARD_KEY))
                .willReturn(LocationCollectionInterval.ONE_MINUTE);

        WardLocationCollectionIntervalInfo result = locationSettingService.getMyCollectionInterval(LocationFixture.WARD_KEY);

        then(locationValidator).should().validateWardRole(LocationFixture.WARD_KEY);
        assertThat(result.currentIntervalSeconds()).isEqualTo(60);
    }
}
