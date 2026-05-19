package com.recaring.notification.implement;

import java.util.List;

public interface FcmClient {
    FcmSendResult send(FcmPushMessage message, List<String> tokens);
}
