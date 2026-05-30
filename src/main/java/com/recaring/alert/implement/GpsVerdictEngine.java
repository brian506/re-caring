package com.recaring.alert.implement;

import com.recaring.alert.vo.GpsAgentReport;
import com.recaring.alert.vo.GpsVerdict;
import com.recaring.alert.vo.GpsVerdictType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GpsVerdictEngine {

    public GpsVerdict synthesize(List<GpsAgentReport> reports) {
        log.info("[GPS 판정 엔진 : 시작]");

        GpsAgentReport infraReport = findAgent(reports, "infra");
        GpsAgentReport sseReport = findAgent(reports, "sse");
        GpsAgentReport deviceReport = findAgent(reports, "device");

        String combinedAnalysis = buildCombinedAnalysis(reports);
        List<String> affectedWardKeys = extractWardKeys(deviceReport);

        // 우선순위: 컨테이너 이슈 > 광범위 장애 > SSE 이슈 > 디바이스 이슈 > 수동 확인
        if (isHighConfidence(infraReport, "CONTAINER_ISSUE")) {
            log.info("[GPS 판정 엔진 : 판정]: verdictType=CONTAINER_ISSUE");
            return new GpsVerdict(
                    GpsVerdictType.CONTAINER_ISSUE,
                    infraReport.analysis(),
                    affectedWardKeys,
                    "서버 컨테이너 문제로 GPS 수신이 중단되었습니다. 자동 복구를 시도합니다.",
                    List.of("docker restart $(docker ps -q | head -1)"),
                    combinedAnalysis
            );
        }

        if (isHighConfidence(infraReport, "WIDESPREAD")) {
            log.info("[GPS 판정 엔진 : 판정]: verdictType=WIDESPREAD");
            return new GpsVerdict(
                    GpsVerdictType.WIDESPREAD,
                    infraReport.analysis(),
                    affectedWardKeys,
                    "다수의 피보호자에게 GPS 수신 문제가 발생했습니다. 서버 인프라를 확인해주세요.",
                    List.of(),
                    combinedAnalysis
            );
        }

        if (isHighConfidence(sseReport, "SSE_DISCONNECTED") || isMediumConfidence(sseReport, "SSE_DISCONNECTED")) {
            log.info("[GPS 판정 엔진 : 판정]: verdictType=SSE_DISCONNECTED");
            return new GpsVerdict(
                    GpsVerdictType.SSE_DISCONNECTED,
                    sseReport.analysis(),
                    affectedWardKeys,
                    "서버 SSE 연결에 문제가 발생했습니다. 클라이언트 재연결을 유도하거나 서비스 재시작을 검토해주세요.",
                    List.of(),
                    combinedAnalysis
            );
        }

        if (isHighConfidence(deviceReport, "DEVICE_TOKEN_ISSUE") || isMediumConfidence(deviceReport, "DEVICE_TOKEN_ISSUE")) {
            log.info("[GPS 판정 엔진 : 판정]: verdictType=DEVICE_TOKEN_ISSUE");
            return new GpsVerdict(
                    GpsVerdictType.DEVICE_TOKEN_ISSUE,
                    deviceReport.analysis(),
                    affectedWardKeys,
                    "피보호자 기기 인증이 만료되었습니다. 피보호자에게 앱 재로그인을 안내해주세요.",
                    List.of(),
                    combinedAnalysis
            );
        }

        if (isHighConfidence(deviceReport, "DEVICE_OFFLINE") || isMediumConfidence(deviceReport, "DEVICE_OFFLINE")) {
            log.info("[GPS 판정 엔진 : 판정]: verdictType=DEVICE_OFFLINE");
            return new GpsVerdict(
                    GpsVerdictType.DEVICE_OFFLINE,
                    deviceReport.analysis(),
                    affectedWardKeys,
                    "피보호자 기기가 오프라인 상태입니다. 배터리 또는 네트워크 연결을 확인해주세요.",
                    List.of(),
                    combinedAnalysis
            );
        }

        log.info("[GPS 판정 엔진 : 판정]: verdictType=NEEDS_HUMAN");
        return new GpsVerdict(
                GpsVerdictType.NEEDS_HUMAN,
                "Agent analysis inconclusive",
                affectedWardKeys,
                "GPS 끊김 원인을 자동으로 파악할 수 없습니다. 수동 확인이 필요합니다.",
                List.of(),
                combinedAnalysis
        );
    }

    private GpsAgentReport findAgent(List<GpsAgentReport> reports, String agentName) {
        return reports.stream()
                .filter(r -> agentName.equals(r.agentName()))
                .findFirst()
                .orElse(new GpsAgentReport(agentName, "Agent not found", "UNKNOWN", "LOW", ""));
    }

    private boolean isHighConfidence(GpsAgentReport report, String rootCauseType) {
        return rootCauseType.equals(report.rootCauseType()) && "HIGH".equals(report.confidence());
    }

    private boolean isMediumConfidence(GpsAgentReport report, String rootCauseType) {
        return rootCauseType.equals(report.rootCauseType()) && "MEDIUM".equals(report.confidence());
    }

    private List<String> extractWardKeys(GpsAgentReport deviceReport) {
        String wardKeys = deviceReport.affectedWardKeys();
        if (wardKeys == null || wardKeys.isBlank()) return List.of();
        return Arrays.stream(wardKeys.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String buildCombinedAnalysis(List<GpsAgentReport> reports) {
        return reports.stream()
                .map(r -> "[" + r.agentName().toUpperCase() + " AGENT]\n" + r.analysis())
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
