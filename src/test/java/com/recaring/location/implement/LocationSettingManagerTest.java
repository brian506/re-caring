package com.recaring.location.implement;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationSettingManager 단위 테스트")
class LocationSettingManagerTest {

    @InjectMocks
    private LocationSettingManager locationSettingManager;

    @Mock
    private LocationSettingRepository locationSettingRepository;

    @Test
    @DisplayName("저장된 설정이 없으면 새 설정을 생성해 위치 수집 주기를 저장한다")
    void updateCollectionInterval_creates_setting_when_absent() {
        given(locationSettingRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        locationSettingManager.updateCollectionInterval(
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.THIRTY_SECONDS
        );

        ArgumentCaptor<LocationSetting> captor = ArgumentCaptor.forClass(LocationSetting.class);
        then(locationSettingRepository).should().save(captor.capture());
        assertThat(captor.getValue().getWardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(captor.getValue().getCollectionIntervalSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("저장된 설정이 있으면 위치 수집 주기를 수정한다")
    void updateCollectionInterval_updates_existing_setting() {
        LocationSetting setting = LocationSetting.builder()
                .wardMemberKey(LocationFixture.WARD_KEY)
                .collectionIntervalSeconds(5)
                .build();
        given(locationSettingRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(setting));

        locationSettingManager.updateCollectionInterval(
                LocationFixture.WARD_KEY,
                LocationCollectionInterval.ONE_MINUTE
        );

        then(locationSettingRepository).should().save(setting);
        assertThat(setting.getCollectionIntervalSeconds()).isEqualTo(60);
    }
}
