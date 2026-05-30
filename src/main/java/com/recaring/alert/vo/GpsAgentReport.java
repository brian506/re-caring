package com.recaring.alert.vo;

public record GpsAgentReport(
        String agentName,
        String analysis,
        String rootCauseType,
        String confidence,
        String affectedWardKeys
) {}
