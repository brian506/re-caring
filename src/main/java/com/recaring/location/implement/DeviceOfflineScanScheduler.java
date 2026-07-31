package com.recaring.location.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Slf4j
@Component
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class DeviceOfflineScanScheduler {

    private final OfflineTimerStore offlineTimerStore;
    private final LocationSettingReader locationSettingReader;
    private final DeviceStatusRequestPublisher deviceStatusRequestPublisher;

    @Scheduled(fixedDelayString = "${device.offline.scan-delay-ms:60000}")
    public void scan() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Set<String> dueMembers = offlineTimerStore.findDue(now);
        if (dueMembers == null || dueMembers.isEmpty()) {
            return;
        }

        for (String memberKey : dueMembers) {
            offlineTimerStore.readLastPoint(memberKey).ifPresent(point -> {
                int collectionIntervalSeconds = locationSettingReader.findCollectionInterval(memberKey).seconds();
                deviceStatusRequestPublisher.publish(memberKey, point, now, collectionIntervalSeconds);
            });
            offlineTimerStore.disarm(memberKey);
        }

        log.info("[기기상태 오프라인 스캔 : 재발행]: count={}", dueMembers.size());
    }
}
