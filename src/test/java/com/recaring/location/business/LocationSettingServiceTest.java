package com.recaring.location.business;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.setting.LocationSettingManager;
import com.recaring.location.implement.LocationValidator;
import com.recaring.location.vo.LocationCollectionInterval;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

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

        then(locationValidator).should().validateCaregiverAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
    }

    @Test
    @DisplayName("보호자가 주기를 수정하면 그 보호자 본인의 설정만 바뀐다")
    void updateCollectionInterval_updates_setting_of_the_requester() {
        locationSettingService.updateCollectionInterval(
                LocationFixture.GUARDIAN_KEY,
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THREE_MINUTES
        );

        then(locationValidator).should().validateCaregiverAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
        then(locationSettingManager).should().updateCollectionInterval(
                LocationFixture.WARD_KEY,
                LocationFixture.GUARDIAN_KEY,
                LocationCollectionInterval.THREE_MINUTES
        );
    }

    @Test
    @DisplayName("보호 대상자와 관계가 없는 회원이 주기를 수정하면 거부되고 설정은 바뀌지 않는다")
    void updateCollectionInterval_does_not_update_when_access_denied() {
        willThrow(new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP))
                .given(locationValidator).validateCaregiverAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);

        assertThatThrownBy(() -> locationSettingService.updateCollectionInterval(
                LocationFixture.GUARDIAN_KEY,
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THREE_MINUTES
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);

        then(locationSettingManager).shouldHaveNoInteractions();
    }
}
