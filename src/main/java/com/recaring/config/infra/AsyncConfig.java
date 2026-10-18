package com.recaring.config.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final long TASK_TERMINATION_TIMEOUT_MILLIS = 20_000L;

    @Bean("broadcastExecutor")
    public Executor broadcastExecutor() {
        return virtualThreadExecutor("broadcast-");
    }

    @Bean("detectionRetryExecutor")
    public Executor detectionRetryExecutor() {
        return virtualThreadExecutor("detection-retry-");
    }

    @Bean("ssePollExecutor")
    public Executor ssePollExecutor() {
        return virtualThreadExecutor("sse-poll-");
    }

    private Executor virtualThreadExecutor(String threadNamePrefix) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setTaskTerminationTimeout(TASK_TERMINATION_TIMEOUT_MILLIS);
        return executor;
    }
}
