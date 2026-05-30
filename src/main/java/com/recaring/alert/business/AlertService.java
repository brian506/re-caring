package com.recaring.alert.business;

import com.recaring.alert.controller.request.AlertWebhookRequest;
import com.recaring.alert.dataaccess.entity.AlertSeverity;
import com.recaring.alert.implement.AlertWebhookValidator;
import com.recaring.alert.implement.GpsDisconnectionOrchestrator;
import com.recaring.alert.vo.AlertItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertWebhookValidator alertWebhookValidator;
    private final GpsDisconnectionOrchestrator gpsDisconnectionOrchestrator;

    public void receiveWebhook(String webhookSecret, AlertWebhookRequest request) {
        alertWebhookValidator.validateSecret(webhookSecret);

        request.alerts().stream()
                .filter(alertItem -> "GpsDataStale".equalsIgnoreCase(
                        alertItem.labels().getOrDefault("alertname", "")))
                .forEach(alertItem -> {
                    AlertSeverity severity = parseSeverity(alertItem.labels().getOrDefault("severity", "CRITICAL"));
                    String fingerprint = alertItem.fingerprint();
                    String message = alertItem.annotations().getOrDefault("summary",
                            alertItem.annotations().getOrDefault("description", "No message"));

                    AlertItem alert = new AlertItem("GpsDataStale", severity, fingerprint, message, alertItem.startsAt());
                    log.info("[웹훅 수신 : GPS 파이프라인 시작]: fingerprint={}", fingerprint);
                    Thread.ofVirtual().start(() -> gpsDisconnectionOrchestrator.orchestrate(alert));
                });
    }

    private AlertSeverity parseSeverity(String severityStr) {
        return switch (severityStr.toUpperCase()) {
            case "INFO" -> AlertSeverity.INFO;
            case "WARNING" -> AlertSeverity.WARNING;
            default -> AlertSeverity.CRITICAL;
        };
    }
}
