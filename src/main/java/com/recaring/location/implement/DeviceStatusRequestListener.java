package com.recaring.location.implement;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.vo.DetectionPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusRequestListener {

    private final LocationSettingReader locationSettingReader;
    private final OfflineTimerStore offlineTimerStore;
    private final DeviceStatusRequestPublisher deviceStatusRequestPublisher;

    @Async("broadcastExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGpsSaved(GpsSavedEvent event) {
        String memberKey = event.memberKey();
        DetectionPoint point = DetectionPoint.from(event.gps());
        int collectionIntervalSeconds = locationSettingReader.findCollectionInterval(memberKey).seconds();

        offlineTimerStore.arm(memberKey, point, collectionIntervalSeconds);
        deviceStatusRequestPublisher.publish(memberKey, point, point.recordedAt(), collectionIntervalSeconds);

        log.info("[기기상태 발행 : GPS 트리거]: memberKey={} | battery={}", memberKey, point.battery());
    }
}
