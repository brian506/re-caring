package com.recaring.alert.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class AwsSsmExecutor implements SsmExecutor {

    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 2000L;

    private final SsmClient ssmClient;

    @Value("${alert.aws.ssm.instance-tag}")
    private String instanceTag;

    @Override
    public String executeReadOnly(String command) {
        log.info("[SSM 실행 : 읽기 전용]: command={}", command);
        return execute(command);
    }

    @Override
    public String executeFix(String command) {
        log.info("[SSM 실행 : 수정 명령]: command={}", command);
        return execute(command);
    }

    private String execute(String command) {
        String instanceId = resolveInstanceId();
        String commandId = sendCommand(instanceId, command);
        return pollForResult(commandId, instanceId);
    }

    private String resolveInstanceId() {
        DescribeInstanceInformationResponse response = ssmClient.describeInstanceInformation(
                DescribeInstanceInformationRequest.builder()
                        .filters(InstanceInformationStringFilter.builder()
                                .key("tag:Name")
                                .values(instanceTag)
                                .build())
                        .build()
        );

        return response.instanceInformationList().stream()
                .findFirst()
                .map(InstanceInformation::instanceId)
                .orElseThrow(() -> {
                    log.error("[SSM 실행 : 인스턴스 조회 실패]: instanceTag={}", instanceTag);
                    return new AppException(ErrorType.ALERT_SSM_COMMAND_FAILED);
                });
    }

    private String sendCommand(String instanceId, String command) {
        SendCommandResponse response = ssmClient.sendCommand(
                SendCommandRequest.builder()
                        .instanceIds(instanceId)
                        .documentName("AWS-RunShellScript")
                        .parameters(java.util.Map.of("commands", List.of(command)))
                        .build()
        );
        return response.command().commandId();
    }

    private String pollForResult(String commandId, String instanceId) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AppException(ErrorType.ALERT_SSM_COMMAND_FAILED);
            }

            GetCommandInvocationResponse invocation = ssmClient.getCommandInvocation(
                    GetCommandInvocationRequest.builder()
                            .commandId(commandId)
                            .instanceId(instanceId)
                            .build()
            );

            CommandInvocationStatus status = invocation.status();
            if (status == CommandInvocationStatus.SUCCESS) {
                String stdout = invocation.standardOutputContent();
                String stderr = invocation.standardErrorContent();
                String combined = stdout + (stderr.isBlank() ? "" : "\nSTDERR: " + stderr);
                log.info("[SSM 실행 : 완료]: commandId={} | status={}", commandId, status);
                return combined;
            }
            if (isFailureStatus(status)) {
                log.warn("[SSM 실행 : 명령 실패]: commandId={} | status={}", commandId, status);
                throw new AppException(ErrorType.ALERT_SSM_COMMAND_FAILED);
            }
        }

        log.error("[SSM 실행 : 타임아웃]: commandId={}", commandId);
        throw new AppException(ErrorType.ALERT_SSM_COMMAND_FAILED);
    }

    private boolean isFailureStatus(CommandInvocationStatus status) {
        return status == CommandInvocationStatus.FAILED
                || status == CommandInvocationStatus.TIMED_OUT
                || status == CommandInvocationStatus.CANCELLED;
    }
}
