package com.recaring.notification.controller.request;

import jakarta.validation.constraints.NotBlank;

public record AcknowledgeAnomalyEventRequest(
        @NotBlank String wardKey,
        @NotBlank String eventId
) {
}
