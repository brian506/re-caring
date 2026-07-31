package com.recaring.common.sqs;

public interface SqsPublisher {
    void send(String queueUrl, Object payload);
}
