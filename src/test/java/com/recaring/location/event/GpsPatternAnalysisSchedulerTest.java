package com.recaring.location.event;

import com.recaring.care.implement.SqsPublisher;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.GpsHistoryReader;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsPatternAnalysisScheduler 단위 테스트")
class GpsPatternAnalysisSchedulerTest {

    @InjectMocks
    private GpsPatternAnalysisScheduler scheduler;

    @Mock
    private GpsHistoryReader gpsHistoryReader;

    @Mock
    private SqsPublisher sqsPublisher;

    @Test
    @DisplayName("활성 WARD가 없으면 SQS에 메시지를 발행하지 않는다")
    void analyzePatterns_skips_when_no_active_wards() {
        ReflectionTestUtils.setField(scheduler, "gpsQueueUrl", "https://sqs.test/queue");
        ReflectionTestUtils.setField(scheduler, "intervalMinutes", 30);
        given(gpsHistoryReader.findActiveWardKeysSince(any(LocalDateTime.class))).willReturn(List.of());

        scheduler.analyzePatterns();

        then(sqsPublisher).should(never()).send(any(), any());
    }

    @Test
    @DisplayName("활성 WARD가 있으면 wardKey별로 SQS 메시지를 발행한다")
    void analyzePatterns_publishes_message_per_active_ward() {
        ReflectionTestUtils.setField(scheduler, "gpsQueueUrl", "https://sqs.test/queue");
        ReflectionTestUtils.setField(scheduler, "intervalMinutes", 30);

        Gps gps = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocalDateTime.now());
        given(gpsHistoryReader.findActiveWardKeysSince(any(LocalDateTime.class)))
                .willReturn(List.of(LocationFixture.WARD_KEY, "ward-key-002"));
        given(gpsHistoryReader.findByWardKeyBetween(
                eq(LocationFixture.WARD_KEY), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(gps));
        given(gpsHistoryReader.findByWardKeyBetween(
                eq("ward-key-002"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(gps));

        scheduler.analyzePatterns();

        then(sqsPublisher).should(times(2)).send(eq("https://sqs.test/queue"), any(GpsPatternSqsMessage.class));
    }
}
