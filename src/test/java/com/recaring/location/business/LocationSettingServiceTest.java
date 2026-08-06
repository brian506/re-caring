package com.recaring.location.business;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.setting.LocationSettingManager;
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
    private LocationSettingManager locationSettingManager;
    @Mock
    private LocationValidator locationValidator;

    @Test
    @DisplayName("주보호자는 피보호자의 현재 위치 수집 주기를 조회한다")
    void getCollectionInterval_returns_current_interval_for_guardian() {
        given(locationSettingManager.findCollectionInterval(LocationFixture.WARD_KEY))
                .willReturn(LocationCollectionInterval.DEFAULT);

        LocationCollectionInterval result = locationSettingService.getCollectionInterval(
                LocationFixture.GUARDIAN_KEY,
                LocationFixture.WARD_KEY
        );

        then(locationValidator).should().validateGuardianAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
        assertThat(result.seconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("주보호자는 위치 수집 주기를 수정한다")
    void updateCollectionInterval_updates_setting_for_guardian() {
        locationSettingService.updateCollectionInterval(
                LocationFixture.GUARDIAN_KEY,
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THIRTY_SECONDS
        );

        then(locationValidator).should().validateGuardianAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        then(locationSettingManager).should().updateCollectionInterval(
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THIRTY_SECONDS
        );
    }

    @Test
    @DisplayName("보호 대상자는 자신의 현재 위치 수집 주기만 조회한다")
    void getMyCollectionInterval_returns_current_interval_only() {
        given(locationSettingManager.findCollectionInterval(LocationFixture.WARD_KEY))
                .willReturn(LocationCollectionInterval.ONE_MINUTE);

        LocationCollectionInterval result = locationSettingService.getMyCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result.seconds()).isEqualTo(60);
    }
}
