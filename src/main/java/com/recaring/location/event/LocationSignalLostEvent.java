package com.recaring.location.event;

import com.recaring.location.vo.Gps;

public record LocationSignalLostEvent(String wardMemberKey, Gps lastGps) {
}
