package com.recaring.location.vo;

import java.util.Arrays;
import java.util.Optional;

public enum AnomalyAlertKind {

    EVENT_START,
    CATEGORY_ADDED,
    REMIND,
    EVENT_END;

    public static Optional<AnomalyAlertKind> find(String value) {
        return Arrays.stream(values())
                .filter(kind -> kind.name().equals(value))
                .findFirst();
    }

    public boolean isPushed() {
        return this != EVENT_END;
    }
}
