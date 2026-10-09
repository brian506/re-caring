package com.recaring.location.implement.gps;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.recaring.location.fixture.LocationFixture.INACCURATE_ACCURACY;
import static com.recaring.location.fixture.LocationFixture.JUMP_15KM_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.LATITUDE;
import static com.recaring.location.fixture.LocationFixture.TRACK_ACCURACY;
import static com.recaring.location.fixture.LocationFixture.TRACK_START;
import static com.recaring.location.fixture.LocationFixture.WALK_50M_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.WARD_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsLatestCacheListener 단위 테스트")
class GpsLatestCacheListenerTest {

    private static final Gps ANCHOR = LocationFixture.createTrackPoint(LATITUDE, TRACK_ACCURACY, TRACK_START);

    @InjectMocks
    private GpsLatestCacheListener listener;

    @Mock
    private GpsLatestCacheManager gpsLatestCacheManager;

    @Test
    @DisplayName("부정확한 좌표도 원본 최신 위치는 갱신하고 표시용 위치는 갱신하지 않는다")
    void inaccurate_point_updates_raw_latest_only() {
        Gps inaccurate = LocationFixture.createTrackPoint(LATITUDE, INACCURATE_ACCURACY, TRACK_START.plusSeconds(30));

        listener.onGpsSaved(new GpsSavedEvent(WARD_KEY, inaccurate));

        then(gpsLatestCacheManager).should().save(WARD_KEY, inaccurate);
        then(gpsLatestCacheManager).should(never()).saveDisplayable(eq(WARD_KEY), any());
    }

    @Test
    @DisplayName("기준점에서 정상적으로 이동한 좌표는 표시용 위치를 갱신한다")
    void walking_point_updates_displayable() {
        Gps walked = LocationFixture.createTrackPoint(
                LATITUDE + WALK_50M_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(30));
        given(gpsLatestCacheManager.findDisplayable(WARD_KEY)).willReturn(Optional.of(ANCHOR));

        listener.onGpsSaved(new GpsSavedEvent(WARD_KEY, walked));

        then(gpsLatestCacheManager).should().saveDisplayable(WARD_KEY, walked);
    }

    @Test
    @DisplayName("기준점에서 튄 좌표는 표시용 위치를 갱신하지 않는다")
    void jumped_point_does_not_update_displayable() {
        Gps jumped = LocationFixture.createTrackPoint(
                LATITUDE + JUMP_15KM_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(30));
        given(gpsLatestCacheManager.findDisplayable(WARD_KEY)).willReturn(Optional.of(ANCHOR));

        listener.onGpsSaved(new GpsSavedEvent(WARD_KEY, jumped));

        then(gpsLatestCacheManager).should(never()).saveDisplayable(eq(WARD_KEY), any());
    }
}
