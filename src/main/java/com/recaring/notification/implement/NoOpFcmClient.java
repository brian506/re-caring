package com.recaring.notification.implement;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFcmClient implements FcmClient {

    @Override
    public List<String> sendAndCollectInvalidTokens(FcmPushMessage message, List<String> tokens) {
        return List.of();
    }
}
