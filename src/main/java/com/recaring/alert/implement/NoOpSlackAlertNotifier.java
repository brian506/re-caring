package com.recaring.alert.implement;

import com.recaring.alert.vo.AlertItem;
import com.recaring.alert.vo.GpsRecoveryResult;
import com.recaring.alert.vo.GpsVerdict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test"})
public class NoOpSlackAlertNotifier implements SlackAlertNotifier {

    @Override
    public String sendInitialAlert(AlertItem alert) {
        log.info("[Slack 알림 : NO-OP 초기 발송]: alertName={} | fingerprint={}", alert.alertName(), alert.fingerprint());
        return "local-thread-ts";
    }

    @Override
    public void sendGpsResolution(String threadTs, GpsVerdict verdict, GpsRecoveryResult recovery) {
        log.info("[Slack 알림 : NO-OP GPS 해결]: threadTs={} | verdictType={} | recovered={}",
                threadTs, verdict.verdictType(), recovery.recovered());
    }

    @Override
    public void sendError(String threadTs, String message) {
        log.info("[Slack 알림 : NO-OP 오류]: threadTs={} | message={}", threadTs, message);
    }
}
