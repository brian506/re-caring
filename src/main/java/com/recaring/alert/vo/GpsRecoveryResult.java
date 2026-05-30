package com.recaring.alert.vo;

public record GpsRecoveryResult(
        boolean recovered,
        String actionTaken,
        String detail
) {}
