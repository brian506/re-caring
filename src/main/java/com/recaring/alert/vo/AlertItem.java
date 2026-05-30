package com.recaring.alert.vo;

import com.recaring.alert.dataaccess.entity.AlertSeverity;

public record AlertItem(
        String alertName,
        AlertSeverity severity,
        String fingerprint,
        String message,
        String startsAt
) {}
