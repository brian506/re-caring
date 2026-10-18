package com.recaring.location.implement.safezone;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.event.SafeZoneEnteredEvent;
import com.recaring.location.event.SafeZoneExitedEvent;
import com.recaring.location.vo.Gps;
import com.recaring.location.vo.SafeZoneObservation;
import com.recaring.location.vo.SafeZoneTransition;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.vo.SafeZoneInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafeZoneDetectionListener {

    private final SafeZoneReader safeZoneReader;
    private final SafeZoneStateManager safeZoneStateManager;
    private final ApplicationEventPublisher eventPublisher;

    @Async("broadcastExecutor")
    @EventListener
    public void onGpsSaved(GpsSavedEvent event) {
        String wardMemberKey = event.memberKey();
        Gps gps = event.gps();

        // 오차가 크면 상태를 갱신하지 않고 빠져나간다. 정확도가 회복되면 마지막으로 신뢰할 수 있었던 상태와 비교된다.
        if (!gps.isAccurate()) {
            return;
        }

        List<SafeZoneInfo> zones = safeZoneReader.findAllByWardMemberKey(wardMemberKey);
        // 안심존 상태 검증은 안심존이 존재할 때만 검사하고 존재 안하면 상태를 삭제
        if (zones.isEmpty()) {
            safeZoneStateManager.delete(wardMemberKey);
            return;
        }

        Optional<SafeZoneTransition> transition =
                safeZoneStateManager.transition(wardMemberKey, SafeZoneObservation.of(zones, gps));

        // 최초 관측은 비교 대상이 없다. 기준선만 세우고 알리지 않는다.
        if (transition.isEmpty()) {
            return;
        }
        publishTransitions(wardMemberKey, zones, transition.get(), gps);
    }

    // 이상탐지 진입 및 이탈 감지
    private void publishTransitions(
            String wardMemberKey,
            List<SafeZoneInfo> zones,
            SafeZoneTransition transition,
            Gps gps
    ) {
        Map<String, SafeZoneInfo> zonesByKey = zones.stream()
                .collect(toMap(SafeZoneInfo::safeZoneKey, Function.identity()));

        // 안심존 진입 알림
        for (String enteredKey : transition.enteredKeys()) {
            SafeZoneInfo zone = zonesByKey.get(enteredKey);
            log.info("[안심존 : 진입]: wardMemberKey={} | safeZoneKey={} | zoneType={}", wardMemberKey, enteredKey, zone.type());
            eventPublisher.publishEvent(new SafeZoneEnteredEvent(
                    wardMemberKey, enteredKey, zone.name(), zone.type(), gps.latitude(), gps.longitude(), gps.occurredAt()));
        }

        // 안심존 이탈 알림
        for (String exitedKey : transition.exitedKeys()) {
            SafeZoneInfo zone = zonesByKey.get(exitedKey);
            // 존이 삭제되어 사라진 경우는 이탈이 아니다.
            if (zone == null) {
                continue;
            }
            log.info("[안심존 : 이탈]: wardMemberKey={} | safeZoneKey={} | zoneType={}", wardMemberKey, exitedKey, zone.type());
            eventPublisher.publishEvent(new SafeZoneExitedEvent(
                    wardMemberKey, exitedKey, zone.name(), zone.type(), gps.latitude(), gps.longitude(), gps.occurredAt()));
        }
    }
}
