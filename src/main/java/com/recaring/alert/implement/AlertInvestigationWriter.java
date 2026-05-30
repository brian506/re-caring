package com.recaring.alert.implement;

import com.recaring.alert.dataaccess.entity.AlertInvestigation;
import com.recaring.alert.dataaccess.entity.AlertSeverity;
import com.recaring.alert.dataaccess.repository.AlertInvestigationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertInvestigationWriter {

    private final AlertInvestigationRepository alertInvestigationRepository;

    public AlertInvestigation createResolved(String fingerprint, String alertName, AlertSeverity severity,
                                             List<String> fixCommands, String analysis) {
        AlertInvestigation investigation = AlertInvestigation.builder()
                .fingerprint(fingerprint)
                .alertName(alertName)
                .severity(severity)
                .build();
        investigation.resolve(fixCommands, analysis);
        AlertInvestigation saved = alertInvestigationRepository.save(investigation);
        log.info("[조사 기록 : 해결 완료 생성]: fingerprint={}", fingerprint);
        return saved;
    }

    public AlertInvestigation createFailed(String fingerprint, String alertName, AlertSeverity severity,
                                           String reason) {
        AlertInvestigation investigation = AlertInvestigation.builder()
                .fingerprint(fingerprint)
                .alertName(alertName)
                .severity(severity)
                .build();
        investigation.fail(reason);
        AlertInvestigation saved = alertInvestigationRepository.save(investigation);
        log.warn("[조사 기록 : 실패 생성]: fingerprint={}", fingerprint);
        return saved;
    }

}
