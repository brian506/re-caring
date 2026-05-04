package com.recaring.care.implement;

public interface SqsPublisher {
    void send(String queueUrl, Object payload);
}
