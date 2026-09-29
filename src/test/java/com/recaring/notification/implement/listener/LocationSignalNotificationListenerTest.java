package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("위치 수집 중단 알림 리스너 단위 테스트")
class LocationSignalNotificationListenerTest {

    private static final String EVENT_TYPE = "LOCATION_SIGNAL_LOST";

    @InjectMocks
    private LocationSignalNotificationListener locationSignalNotificationListener;

    @Mock
    private CareRelationshipReader careRelationshipReader;
    @Mock
    private NotificationSendManager notificationSendManager;
    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("보호자와 관계자에게 마지막 위치를 담아 위치 수집 중단을 알린다")
    void sends_signal_lost_notification_with_last_location() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.PRIMARY_GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.OTHER_GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.WARD_NAME);

        locationSignalNotificationListener.onLocationSignalLost(LocationFixture.createLocationSignalLostEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY, NotificationFixture.OTHER_GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                EVENT_TYPE,
                "위치 수집 중단 알림",
                "김소연님의 위치가 20분 넘게 수집되지 않고 있어요. 기기 전원과 앱 로그인 상태를 확인해 주세요.",
                Map.of(
                        "type", EVENT_TYPE,
                        "wardKey", NotificationFixture.WARD_KEY,
                        "lastReceivedAt", LocationFixture.RECORDED_AT_TEXT,
                        "recordedAt", LocationFixture.MEASURED_AT_TEXT,
                        "latitude", String.valueOf(LocationFixture.LATITUDE),
                        "longitude", String.valueOf(LocationFixture.LONGITUDE)
                )
        );
    }

    @Test
    @DisplayName("수신자가 없으면 알리지 않는다")
    void skips_when_no_caregiver() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY)).willReturn(List.of());

        locationSignalNotificationListener.onLocationSignalLost(LocationFixture.createLocationSignalLostEvent());

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
    }
}
