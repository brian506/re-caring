package com.recaring.notification.implement.fcm;

import java.util.List;

public interface FcmClient {

    List<String> sendAndCollectInvalidTokens(FcmPushMessage message, List<String> tokens);
}
