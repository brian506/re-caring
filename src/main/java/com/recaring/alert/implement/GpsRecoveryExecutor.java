package com.recaring.alert.implement;

import com.recaring.alert.vo.GpsRecoveryResult;
import com.recaring.alert.vo.GpsVerdict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsRecoveryExecutor {

    private final SsmExecutor ssmExecutor;
    private final RunbookManager runbookManager;

    public GpsRecoveryResult execute(GpsVerdict verdict) {
        log.info("[GPS 복구 실행 : 시작]: verdictType={}", verdict.verdictType());

        return switch (verdict.verdictType()) {
            case CONTAINER_ISSUE -> restartContainer(verdict);
            case DEVICE_OFFLINE, DEVICE_TOKEN_ISSUE, SSE_DISCONNECTED, WIDESPREAD, NEEDS_HUMAN -> notifyOnly(verdict);
        };
    }

    private GpsRecoveryResult restartContainer(GpsVerdict verdict) {
        try {
            for (String command : verdict.recoveryCommands()) {
                log.info("[GPS 복구 실행 : 명령 실행]: command={}", command);
                ssmExecutor.executeFix(command);
            }

            runbookManager.save(
                    "GpsDataStale",
                    verdict.verdictType().name(),
                    verdict.recoveryCommands(),
                    verdict.rootCause()
            );

            log.info("[GPS 복구 실행 : 컨테이너 재시작 완료]");
            return new GpsRecoveryResult(true, "컨테이너 재시작 완료", String.join(", ", verdict.recoveryCommands()));
        } catch (Exception e) {
            log.error("[GPS 복구 실행 : 재시작 실패]: error={}", e.getMessage());
            return new GpsRecoveryResult(false, "컨테이너 재시작 실패", e.getMessage());
        }
    }

    private GpsRecoveryResult notifyOnly(GpsVerdict verdict) {
        log.info("[GPS 복구 실행 : 알림 전용]: verdictType={}", verdict.verdictType());
        return new GpsRecoveryResult(false, "알림 전용", verdict.caregiverMessage());
    }
}
