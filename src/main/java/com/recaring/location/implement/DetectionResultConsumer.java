package com.recaring.location.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.event.DeviceStatusAlertEvent;
import com.recaring.location.vo.DetectionResultMessage;
import com.recaring.location.vo.DeviceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Slf4j
@Component
@Profile({"prod", "dev"})
@RequiredArgsConstructor
public class DetectionResultConsumer {

    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 20;

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final DeviceStateReader deviceStateReader;
    private final DeviceStateWriter deviceStateWriter;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${aws.sqs.detection-result-queue-url:}")
    private String detectionResultQueueUrl;

    @Scheduled(fixedDelayString = "${aws.sqs.detection-result-poll-delay-ms:1000}")
    public void poll() {
        if (!StringUtils.hasText(detectionResultQueueUrl)) {
            return;
        }

        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(detectionResultQueueUrl)
                        .maxNumberOfMessages(MAX_MESSAGES)
                        .waitTimeSeconds(WAIT_TIME_SECONDS)
                        .build())
                .messages();

        for (Message message : messages) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            DetectionResultMessage result = objectMapper.readValue(message.body(), DetectionResultMessage.class);
            process(result);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(detectionResultQueueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception e) {
            log.error("[기기상태 결과 : 처리 실패]: messageId={} | error={}", message.messageId(), e.getMessage());
        }
    }

    private void process(DetectionResultMessage result) {
        String memberKey = result.memberKey();
        DeviceState stored = deviceStateReader.find(memberKey);
        if (stored == result.newState()) {
            log.info("[기기상태 결과 : 상태 동일 스킵]: memberKey={} | state={}", memberKey, stored);
            return;
        }

        deviceStateWriter.save(memberKey, result.newState());
        log.info("[기기상태 결과 : 상태 전이]: memberKey={} | before={} | after={}", memberKey, stored, result.newState());

        if (result.newState() == DeviceState.LOW_BATTERY
                || result.newState() == DeviceState.FULL_BATTERY
                || result.newState() == DeviceState.OFFLINE) {
            eventPublisher.publishEvent(
                    new DeviceStatusAlertEvent(memberKey, result.newState(), result.detectedAt()));
        }
    }
}
