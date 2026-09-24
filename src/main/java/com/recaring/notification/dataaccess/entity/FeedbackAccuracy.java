package com.recaring.notification.dataaccess.entity;

public enum FeedbackAccuracy {

    ACCURATE(false),
    INACCURATE(true),
    UNSURE(false);

    private final boolean reasonRequired;

    FeedbackAccuracy(boolean reasonRequired) {
        this.reasonRequired = reasonRequired;
    }

    public boolean isReasonRequired() {
        return reasonRequired;
    }
}
