package com.recaring.notification.controller.request;

import com.recaring.notification.dataaccess.entity.FeedbackAccuracy;
import com.recaring.notification.dataaccess.entity.FeedbackReason;
import com.recaring.notification.vo.NotificationFeedbackAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitNotificationFeedbackRequest(
        @Schema(description = "알림 정확도 응답", example = "INACCURATE")
        @NotNull FeedbackAccuracy accuracy,

        @Schema(description = "정확하지 않은 이유. accuracy=INACCURATE일 때만 보낸다", example = "GPS_INACCURATE")
        FeedbackReason reason,

        @Schema(description = "추가 의견 (선택, 최대 100자)")
        @Size(max = 100) String comment
) {
    public NotificationFeedbackAnswer toAnswer() {
        return new NotificationFeedbackAnswer(accuracy, reason, comment);
    }
}
