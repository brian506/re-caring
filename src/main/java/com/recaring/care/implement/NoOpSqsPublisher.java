package com.recaring.care.implement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test"})
public class NoOpSqsPublisher implements SqsPublisher {

    @Override
    public void send(String queueUrl, Object payload) {
        log.info("[SQS 발행 : LOCAL 스킵]: payload={}", payload);
    }
}
