package com.recaring.location.event;

import com.recaring.location.vo.Gps;

public record GpsReceivedEvent(String wardMemberKey, Gps gps) {}
