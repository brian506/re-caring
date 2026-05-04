package com.recaring.location.event;

import com.recaring.location.implement.GpsLatestCacheWriter;
import com.recaring.location.implement.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class GpsEventHandler {

    private final GpsLatestCacheWriter gpsLatestCacheWriter;
    private final SseEmitterManager sseEmitterManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(GpsReceivedEvent event) {
        gpsLatestCacheWriter.save(event.wardMemberKey(), event.gps());
        sseEmitterManager.broadcast(event.wardMemberKey(), event.gps());
    }
}
