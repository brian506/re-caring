package com.recaring.alert.implement;

import com.recaring.alert.vo.AlertItem;
import com.recaring.alert.vo.GpsAgentReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsDeviceAnalysisAgent {

    private static final String AGENT_NAME = "device";
    private static final Pattern JSON_FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"");

    private final ChatClient chatClient;
    private final GpsTools gpsTools;

    @Value("${alert.prompt.gps.device-system:classpath:prompts/gps/device-agent-system.st}")
    private Resource systemPromptResource;

    @Value("${alert.prompt.gps.device-user:classpath:prompts/gps/device-agent-user.st}")
    private Resource userPromptResource;

    public GpsAgentReport analyze(AlertItem alert) {
        log.info("[GPS 디바이스 에이전트 : 시작]: fingerprint={}", alert.fingerprint());
        try {
            String response = chatClient.prompt()
                    .system(systemPromptResource)
                    .user(buildUserPrompt(alert))
                    .tools(gpsTools)
                    .call()
                    .content();

            log.info("[GPS 디바이스 에이전트 : 완료]: fingerprint={}", alert.fingerprint());
            return parseReport(response);
        } catch (Exception e) {
            log.error("[GPS 디바이스 에이전트 : 실패]: fingerprint={} | error={}", alert.fingerprint(), e.getMessage());
            return new GpsAgentReport(AGENT_NAME, "Analysis failed: " + e.getMessage(), "UNKNOWN", "LOW", "");
        }
    }

    private String buildUserPrompt(AlertItem alert) {
        return new PromptTemplate(userPromptResource).render(Map.of(
                "alertName", alert.alertName(),
                "severity", alert.severity().name(),
                "message", alert.message(),
                "startsAt", alert.startsAt()
        ));
    }

    private GpsAgentReport parseReport(String response) {
        Map<String, String> fields = extractLastJsonBlock(response);
        return new GpsAgentReport(
                AGENT_NAME,
                response,
                fields.getOrDefault("rootCauseType", "UNKNOWN"),
                fields.getOrDefault("confidence", "LOW"),
                fields.getOrDefault("affectedWardKeys", "")
        );
    }

    private Map<String, String> extractLastJsonBlock(String response) {
        int start = response.lastIndexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) return Map.of();

        Map<String, String> result = new HashMap<>();
        Matcher m = JSON_FIELD.matcher(response.substring(start, end + 1));
        while (m.find()) result.put(m.group(1), m.group(2));
        return result;
    }
}
