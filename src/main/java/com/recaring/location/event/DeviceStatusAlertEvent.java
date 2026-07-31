package com.recaring.location.event;

import com.recaring.location.vo.DeviceState;

import java.time.LocalDateTime;

public record DeviceStatusAlertEvent(String memberKey, DeviceState newState, LocalDateTime detectedAt) {
}
