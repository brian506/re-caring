package com.recaring.location.implement.gps;

import com.recaring.location.event.GpsSavedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GpsLatestCacheListener {

    private final GpsLatestCacheManager gpsLatestCacheManager;

    @Async("broadcastExecutor")
    @EventListener
    public void onGpsSaved(GpsSavedEvent event) {
        gpsLatestCacheManager.save(event.memberKey(), event.gps());
    }
}
