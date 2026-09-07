package com.recaring.notification.controller.response;

import com.recaring.notification.vo.NotificationSlice;

import java.util.List;

public record NotificationSliceResponse(
        List<NotificationResponse> items,
        Long nextCursor,
        boolean hasNext
) {
    public static NotificationSliceResponse from(NotificationSlice slice) {
        return new NotificationSliceResponse(
                slice.items().stream().map(NotificationResponse::from).toList(),
                slice.nextCursor(),
                slice.hasNext()
        );
    }
}
