package com.recaring.location.implement.detection;

import com.recaring.location.event.GpsSavedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionListener {

    private final DetectionPublisher detectionPublisher;

    @Async("broadcastExecutor")
    @EventListener
    public void onGpsSaved(GpsSavedEvent event) {
        detectionPublisher.publish(event.memberKey(), event.gps());
    }
}
