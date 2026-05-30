package com.recaring.alert.vo;

import com.recaring.alert.dataaccess.entity.InvestigationStatus;

import java.time.Instant;
import java.util.List;

public record InvestigationState(
        String threadTs,
        InvestigationStatus status,
        Instant startedAt,
        List<String> fixCommands
) {}
