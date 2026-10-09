package com.recaring.location.implement.gps;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsLatestCacheListener {

    private final GpsLatestCacheManager gpsLatestCacheManager;

    @Async("broadcastExecutor")
    @EventListener
    public void onGpsSaved(GpsSavedEvent event) {
        String wardMemberKey = event.memberKey();
        Gps gps = event.gps();
        gpsLatestCacheManager.save(wardMemberKey, gps);

        Gps anchor = gpsLatestCacheManager.findDisplayable(wardMemberKey).orElse(null);
        if (gps.isDisplayableAfter(anchor)) {
            gpsLatestCacheManager.saveDisplayable(wardMemberKey, gps);
            return;
        }
        log.debug("[GPS 캐시 : 표시 제외]: wardMemberKey={} | accuracy={}", wardMemberKey, gps.accuracy());
    }
}
