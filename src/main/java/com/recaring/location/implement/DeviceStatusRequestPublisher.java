package com.recaring.location.implement;

import com.recaring.common.sqs.SqsPublisher;
import com.recaring.location.vo.DetectionPoint;
import com.recaring.location.vo.DeviceState;
import com.recaring.location.vo.GpsDetectionMessage;
import com.recaring.notification.implement.NotificationSettingReader;
import com.recaring.notification.vo.BatteryThresholdRange;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeviceStatusRequestPublisher {

    private final DeviceStateReader deviceStateReader;
    private final NotificationSettingReader notificationSettingReader;
    private final SqsPublisher sqsPublisher;

    @Value("${aws.sqs.detection-request-queue-url:}")
    private String detectionRequestQueueUrl;

    public void publish(String memberKey, DetectionPoint currentLocation, LocalDateTime evaluationTime, int collectionIntervalSeconds) {
        DeviceState currentState = deviceStateReader.find(memberKey);
        BatteryThresholdRange batteryThresholds = notificationSettingReader.findBatteryThresholds(memberKey);
        GpsDetectionMessage message = GpsDetectionMessage.of(
                memberKey, currentLocation, collectionIntervalSeconds, currentState,
                batteryThresholds.low().percent(), batteryThresholds.full().percent(), evaluationTime);
        sqsPublisher.send(detectionRequestQueueUrl, message);
    }
}
