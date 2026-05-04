package com.recaring.location.event;

import com.recaring.care.implement.SqsPublisher;
import com.recaring.location.implement.GpsHistoryReader;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsPatternAnalysisScheduler {

    private final GpsHistoryReader gpsHistoryReader;
    private final SqsPublisher sqsPublisher;

    @Value("${aws.sqs.gps-queue-url:}")
    private String gpsQueueUrl;

    @Value("${gps.pattern.analysis.interval-minutes:30}")
    private int intervalMinutes;

    // 기본 30분마다 실행. gps.pattern.analysis.interval-ms 로 오버라이드 가능
    @Scheduled(fixedRateString = "${gps.pattern.analysis.interval-ms:1800000}")
    public void analyzePatterns() {
        LocalDateTime from = LocalDateTime.now().minusMinutes(intervalMinutes);
        LocalDateTime to = LocalDateTime.now();

        List<String> activeWardKeys = gpsHistoryReader.findActiveWardKeysSince(from);
        if (activeWardKeys.isEmpty()) {
            return;
        }

        for (String wardKey : activeWardKeys) {
            List<Gps> gpsHistory = gpsHistoryReader.findByWardKeyBetween(wardKey, from, to);
            GpsPatternSqsMessage message = GpsPatternSqsMessage.from(wardKey, gpsHistory);
            sqsPublisher.send(gpsQueueUrl, message);
            log.info("[GPS 패턴 분석 : SQS 발행 완료]: wardKey={} | points={}", wardKey, gpsHistory.size());
        }
    }
}
