package com.recaring.location.implement;

import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.LocationCollectionInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationSettingReader 단위 테스트")
class LocationSettingReaderTest {

    @InjectMocks
    private LocationSettingReader locationSettingReader;

    @Mock
    private LocationSettingRepository locationSettingRepository;

    @Test
    @DisplayName("저장된 설정이 없으면 기본 위치 수집 주기를 반환한다")
    void findCollectionInterval_returns_default_when_setting_absent() {
        given(locationSettingRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        LocationCollectionInterval result = locationSettingReader.findCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.DEFAULT);
    }

    @Test
    @DisplayName("저장된 설정이 있으면 해당 위치 수집 주기를 반환한다")
    void findCollectionInterval_returns_saved_interval() {
        LocationSetting setting = LocationSetting.builder()
                .wardMemberKey(LocationFixture.WARD_KEY)
                .collectionIntervalSeconds(30)
                .build();
        given(locationSettingRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(setting));

        LocationCollectionInterval result = locationSettingReader.findCollectionInterval(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
    }
}
