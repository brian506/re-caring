package com.recaring.config.infra;

import com.recaring.location.implement.detection.AnomalyDetectionConsumer;
import com.recaring.location.implement.detection.AnomalyStreamProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisStreamConfig {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> anomalyStreamContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redisTemplate,
            AnomalyDetectionConsumer anomalyDetectionConsumer
    ) {
        createGroupIfAbsent(redisTemplate);

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(
                        connectionFactory,
                        StreamMessageListenerContainerOptions.builder()
                                .pollTimeout(POLL_TIMEOUT)
                                .build());

        // receive()는 수동 ACK다. 컨슈머가 알림 저장을 끝낸 뒤 직접 XACK한다.
        container.receive(
                Consumer.from(AnomalyStreamProperties.GROUP_NAME, AnomalyStreamProperties.CONSUMER_NAME),
                StreamOffset.create(AnomalyStreamProperties.STREAM_KEY, ReadOffset.lastConsumed()),
                anomalyDetectionConsumer);

        container.start();
        return container;
    }

    // 그룹은 자동 생성되지 않는다. 없으면 XREADGROUP이 NOGROUP으로 실패한다.
    // createGroup은 스트림이 없으면 MKSTREAM으로 함께 만든다. 엔진이 첫 결과를 넣기 전에 기동해도 된다.
    // 시작 지점은 '$'(그룹 생성 이후 도착분만)다. 이미 쌓인 지난 알림을 뒤늦게 몰아 보내지 않기 위함이다.
    private void createGroupIfAbsent(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.opsForStream().createGroup(
                    AnomalyStreamProperties.STREAM_KEY, AnomalyStreamProperties.GROUP_NAME);
            log.info("[이상탐지 스트림 : 컨슈머 그룹 생성]: group={}", AnomalyStreamProperties.GROUP_NAME);
        } catch (DataAccessException e) {
            // BUSYGROUP은 이미 있다는 뜻이라 정상이다. 재기동·다중 태스크마다 발생하므로 여기서 던지면
            // 두 번째 배포부터 기동이 막힌다. 그 외(연결 실패 등)는 컨슈머만 못 뜨고 GPS 수신은 살아 있어야 하므로
            // 기동을 막지 않고 error로만 남긴다.
            if (isGroupAlreadyExists(e)) {
                log.info("[이상탐지 스트림 : 컨슈머 그룹 존재]: group={}", AnomalyStreamProperties.GROUP_NAME);
                return;
            }
            log.error("[이상탐지 스트림 : 컨슈머 그룹 생성 실패]: group={} | error={}",
                    AnomalyStreamProperties.GROUP_NAME, rootMessage(e));
        }
    }

    private String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private boolean isGroupAlreadyExists(DataAccessException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("BUSYGROUP")) {
                return true;
            }
        }
        return false;
    }
}
