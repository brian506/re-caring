package com.recaring.alert.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Slf4j
@Component
public class SlackSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "v0=";
    private static final long MAX_TIMESTAMP_DIFF_SECONDS = 300;

    @Value("${alert.slack.signing-secret}")
    private String signingSecret;

    public void validate(String timestamp, String signature, String body) {
        validateTimestamp(timestamp);
        validateSignature(timestamp, signature, body);
    }

    private void validateTimestamp(String timestamp) {
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            long currentTimestamp = System.currentTimeMillis() / 1000;
            if (Math.abs(currentTimestamp - requestTimestamp) > MAX_TIMESTAMP_DIFF_SECONDS) {
                log.warn("[Slack 서명 검증 : 타임스탬프 만료]: timestamp={}", timestamp);
                throw new AppException(ErrorType.INVALID_ALERT_WEBHOOK_SIGNATURE);
            }
        } catch (NumberFormatException e) {
            log.warn("[Slack 서명 검증 : 타임스탬프 형식 오류]: timestamp={}", timestamp);
            throw new AppException(ErrorType.INVALID_ALERT_WEBHOOK_SIGNATURE);
        }
    }

    private void validateSignature(String timestamp, String signature, String body) {
        try {
            String baseString = "v0:" + timestamp + ":" + body;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = SIGNATURE_PREFIX + HexFormat.of().formatHex(hash);

            if (!expectedSignature.equals(signature)) {
                log.warn("[Slack 서명 검증 : 서명 불일치]");
                throw new AppException(ErrorType.INVALID_ALERT_WEBHOOK_SIGNATURE);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Slack 서명 검증 : 오류]: error={}", e.getMessage());
            throw new AppException(ErrorType.INVALID_ALERT_WEBHOOK_SIGNATURE);
        }
    }
}
