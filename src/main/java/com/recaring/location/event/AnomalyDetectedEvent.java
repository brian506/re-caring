package com.recaring.location.event;

import com.recaring.location.vo.AnomalyAlert;

public record AnomalyDetectedEvent(AnomalyAlert alert) {
}
