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
    @DisplayName("보호자는 자기가 설정한 위치 수신 주기를 조회한다")
    void getCollectionInterval_returns_interval_of_the_requester() {
        given(locationSettingManager.findCollectionInterval(LocationFixture.WARD_KEY, LocationFixture.GUARDIAN_KEY))
                .willReturn(LocationCollectionInterval.THIRTY_SECONDS);

        LocationCollectionInterval result = locationSettingService.getCollectionInterval(
                LocationFixture.GUARDIAN_KEY,
                LocationFixture.WARD_KEY
        );

        then(locationValidator).should().validateGuardianAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
    }

    @Test
    @DisplayName("보호자가 주기를 수정하면 그 보호자 본인의 설정만 바뀐다")
    void updateCollectionInterval_updates_setting_of_the_requester() {
        locationSettingService.updateCollectionInterval(
                LocationFixture.GUARDIAN_KEY,
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THIRTY_SECONDS
        );

        then(locationValidator).should().validateGuardianAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        then(locationSettingManager).should().updateCollectionInterval(
                LocationFixture.WARD_KEY,
                LocationFixture.GUARDIAN_KEY,
                LocationCollectionInterval.THIRTY_SECONDS
        );
    }

    @Test
    @DisplayName("보호 대상자 기기는 보호자들 설정에서 정해진 기기 수집 주기를 받는다")
    void getMyCollectionInterval_returns_device_collection_interval() {
        given(locationSettingManager.findDeviceCollectionInterval(LocationFixture.WARD_KEY))
                .willReturn(LocationCollectionInterval.ONE_MINUTE);

        LocationCollectionInterval result = locationSettingService.getMyCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result.seconds()).isEqualTo(60);
    }
}
