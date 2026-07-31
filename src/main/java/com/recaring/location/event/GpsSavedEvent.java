package com.recaring.location.event;

import com.recaring.location.vo.Gps;

public record GpsSavedEvent(String memberKey, Gps gps) {
}
