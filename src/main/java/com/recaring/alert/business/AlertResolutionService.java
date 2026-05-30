package com.recaring.alert.business;

import com.recaring.alert.implement.SlackSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertResolutionService {

    private final SlackSignatureValidator slackSignatureValidator;

    public void handleSlackAction(String timestamp, String signature, String body) {
        slackSignatureValidator.validate(timestamp, signature, body);
        log.info("[Slack 액션 : 수신]: body_length={}", body.length());
        // Slack interactive action handling — extensible for future button/menu actions
    }
}
