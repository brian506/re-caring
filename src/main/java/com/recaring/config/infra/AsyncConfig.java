package com.recaring.config.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("broadcastExecutor")
    public Executor broadcastExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("broadcast-", 0).factory()
        );
    }

    @Bean("ssePollExecutor")
    public Executor ssePollExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("sse-poll-", 0).factory()
        );
    }
}
