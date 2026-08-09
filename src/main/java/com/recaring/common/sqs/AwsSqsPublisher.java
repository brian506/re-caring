package com.recaring.common.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class AwsSqsPublisher implements SqsPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String queueUrl, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .build());
        } catch (Exception e) {
            //todo 제시도 로직 구현 필요
            log.error("[SQS 발행 : 실패]: queueUrl={} | error={}", queueUrl, e.getMessage());
        }
    }
}
