package com.recaring.alert.controller.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AlertWebhookRequest(
        String version,
        String groupKey,
        String status,
        @NotNull List<AlertItemRequest> alerts
) {}
