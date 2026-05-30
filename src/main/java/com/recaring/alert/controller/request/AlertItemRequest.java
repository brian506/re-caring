package com.recaring.alert.controller.request;

import java.util.Map;

public record AlertItemRequest(
        String fingerprint,
        String startsAt,
        String endsAt,
        String status,
        Map<String, String> labels,
        Map<String, String> annotations
) {}
