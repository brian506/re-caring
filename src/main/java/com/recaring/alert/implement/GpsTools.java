package com.recaring.alert.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsTools {

    private final SsmExecutor ssmExecutor;
    private final PrometheusContextFetcher prometheusContextFetcher;
    private final RunbookManager runbookManager;

    @Tool(description = "Run a read-only diagnostic command on EC2 via AWS SSM. Use for: DB queries (docker exec psql), log inspection (docker logs --tail N <container>), container status (docker inspect), environment variables (docker exec <container> env).")
    public String fetchSsmLogs(String command) {
        log.info("[GpsTools : fetchSsmLogs]: command={}", command);
        return ssmExecutor.executeReadOnly(command);
    }

    @Tool(description = "Query Prometheus metrics using PromQL. Useful for GPS write rate on /api/v1/location/gps, HTTP request counts, JVM memory, or container CPU metrics.")
    public String queryPrometheus(String promql, String timeRange) {
        log.info("[GpsTools : queryPrometheus]: promql={}", promql);
        return prometheusContextFetcher.query(promql, timeRange);
    }

    @Tool(description = "Save a GPS recovery runbook for future reuse. Provide alertName (use 'GpsDataStale'), errorSignature (root cause type e.g. 'CONTAINER_ISSUE'), commandsJson (comma-separated fix commands), and resolutionContext summary.")
    public String saveRunbook(String alertName, String errorSignature, String commandsJson, String resolutionContext) {
        log.info("[GpsTools : saveRunbook]: alertName={} | errorSignature={}", alertName, errorSignature);
        runbookManager.save(alertName, errorSignature, Arrays.asList(commandsJson.split(",\\s*")), resolutionContext);
        return "GPS runbook saved.";
    }
}
