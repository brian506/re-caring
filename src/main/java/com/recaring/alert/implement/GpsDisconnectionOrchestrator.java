package com.recaring.alert.implement;

import com.recaring.alert.dataaccess.entity.InvestigationStatus;
import com.recaring.alert.vo.AlertItem;
import com.recaring.alert.vo.GpsAgentReport;
import com.recaring.alert.vo.GpsRecoveryResult;
import com.recaring.alert.vo.GpsVerdict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsDisconnectionOrchestrator {

    private final InvestigationStateReader stateReader;
    private final InvestigationStateWriter stateWriter;
    private final AlertInvestigationWriter investigationWriter;
    private final SlackAlertNotifier slackAlertNotifier;
    private final GpsDeviceAnalysisAgent deviceAnalysisAgent;
    private final GpsSseAnalysisAgent sseAnalysisAgent;
    private final GpsInfraAnalysisAgent infraAnalysisAgent;
    private final GpsVerdictEngine verdictEngine;
    private final GpsRecoveryExecutor recoveryExecutor;

    private final Executor agentExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public void orchestrate(AlertItem alert) {
        String fingerprint = alert.fingerprint();
        log.info("[GPS 오케스트레이터 : 시작]: fingerprint={}", fingerprint);

        if (isDuplicateRunning(fingerprint)) {
            log.info("[GPS 오케스트레이터 : 중복 스킵]: fingerprint={}", fingerprint);
            return;
        }

        String threadTs = slackAlertNotifier.sendInitialAlert(alert);
        stateWriter.saveRunning(fingerprint, threadTs);

        try {
            List<GpsAgentReport> reports = runAgentsInParallel(alert);
            GpsVerdict verdict = verdictEngine.synthesize(reports);
            GpsRecoveryResult recovery = recoveryExecutor.execute(verdict);

            slackAlertNotifier.sendGpsResolution(threadTs, verdict, recovery);
            investigationWriter.createResolved(
                    fingerprint, alert.alertName(), alert.severity(),
                    verdict.recoveryCommands(), verdict.combinedAnalysis());
            stateWriter.delete(fingerprint);

            log.info("[GPS 오케스트레이터 : 완료]: fingerprint={} | verdictType={}", fingerprint, verdict.verdictType());
        } catch (Exception e) {
            log.error("[GPS 오케스트레이터 : 실패]: fingerprint={} | error={}", fingerprint, e.getMessage());
            slackAlertNotifier.sendError(threadTs, e.getMessage());
            investigationWriter.createFailed(fingerprint, alert.alertName(), alert.severity(), e.getMessage());
            stateWriter.delete(fingerprint);
        }
    }

    private boolean isDuplicateRunning(String fingerprint) {
        return stateReader.findByFingerprint(fingerprint)
                .map(state -> state.status() == InvestigationStatus.RUNNING)
                .orElse(false);
    }

    private List<GpsAgentReport> runAgentsInParallel(AlertItem alert) {
        log.info("[GPS 에이전트 : 병렬 실행 시작]");
        CompletableFuture<GpsAgentReport> deviceFuture =
                CompletableFuture.supplyAsync(() -> deviceAnalysisAgent.analyze(alert), agentExecutor);
        CompletableFuture<GpsAgentReport> sseFuture =
                CompletableFuture.supplyAsync(() -> sseAnalysisAgent.analyze(alert), agentExecutor);
        CompletableFuture<GpsAgentReport> infraFuture =
                CompletableFuture.supplyAsync(() -> infraAnalysisAgent.analyze(alert), agentExecutor);

        return List.of(deviceFuture.join(), sseFuture.join(), infraFuture.join());
    }
}
