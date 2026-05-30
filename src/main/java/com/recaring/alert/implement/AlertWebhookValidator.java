package com.recaring.alert.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlertWebhookValidator {

    @Value("${alert.webhook.secret}")
    private String webhookSecret;

    public void validateSecret(String providedSecret) {
        if (providedSecret == null || !providedSecret.equals(webhookSecret)) {
            log.warn("[웹훅 검증 : 서명 실패]");
            throw new AppException(ErrorType.INVALID_ALERT_WEBHOOK_SIGNATURE);
        }
    }
}
