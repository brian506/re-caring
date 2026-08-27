package com.recaring.location.implement.detection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public final class AnomalyStreamProperties {

    public static final String STREAM_KEY = "anomaly-alerts";
    public static final String GROUP_NAME = "recaring-backend";

    // 태스크마다 달라야 한다. 같은 이름을 둘이 쓰면 서로의 미처리 메시지를 가져가 PEL이 뒤섞인다.
    // 컨테이너 소비와 PEL 회수가 같은 이름을 써야 소유자가 갈리지 않으므로 한 번만 계산해 공유한다.
    public static final String CONSUMER_NAME = resolveConsumerName();

    private AnomalyStreamProperties() {
    }

    private static String resolveConsumerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "recaring-" + UUID.randomUUID();
        }
    }
}
