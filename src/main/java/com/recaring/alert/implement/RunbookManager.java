package com.recaring.alert.implement;

import com.recaring.alert.dataaccess.entity.AlertRunbook;
import com.recaring.alert.dataaccess.repository.AlertRunbookRepository;
import com.recaring.alert.vo.RunbookInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunbookManager {

    private final AlertRunbookRepository alertRunbookRepository;

    public Optional<RunbookInfo> findByAlertNameAndErrorSignature(String alertName, String errorSignature) {
        Optional<RunbookInfo> result = alertRunbookRepository
                .findByAlertNameAndErrorSignature(alertName, errorSignature)
                .map(RunbookInfo::from);
        if (result.isPresent()) {
            log.info("[런북 캐시 : 히트]: alertName={} | errorSignature={}", alertName, errorSignature);
        } else {
            log.info("[런북 캐시 : 미스]: alertName={} | errorSignature={}", alertName, errorSignature);
        }
        return result;
    }

    @Transactional
    public void save(String alertName, String errorSignature, List<String> commands, String resolutionContext) {
        AlertRunbook runbook = AlertRunbook.builder()
                .alertName(alertName)
                .errorSignature(errorSignature)
                .commands(commands)
                .resolutionContext(resolutionContext)
                .build();
        alertRunbookRepository.save(runbook);
        log.info("[런북 : 저장 완료]: alertName={}", alertName);
    }

    @Transactional
    public void incrementSuccess(Long runbookId) {
        AlertRunbook runbook = alertRunbookRepository.findById(runbookId)
                .orElseThrow(() -> new AppException(ErrorType.ALERT_RUNBOOK_NOT_FOUND));
        runbook.incrementSuccess();
        alertRunbookRepository.save(runbook);
        log.info("[런북 : 성공 횟수 증가]: runbookId={}", runbookId);
    }
}
