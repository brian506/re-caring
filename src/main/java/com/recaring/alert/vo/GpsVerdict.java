package com.recaring.alert.vo;

import java.util.List;

public record GpsVerdict(
        GpsVerdictType verdictType,
        String rootCause,
        List<String> affectedWardKeys,
        String caregiverMessage,
        List<String> recoveryCommands,
        String combinedAnalysis
) {}
