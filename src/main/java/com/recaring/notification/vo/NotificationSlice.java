package com.recaring.notification.vo;

import java.util.List;

public record NotificationSlice(
        List<NotificationItem> items,
        Long nextCursor,
        boolean hasNext
) {
    // fetched는 size + 1건까지 담겨 올 수 있다. 초과분은 다음 페이지 존재 신호로만 쓰고 잘라낸다.
    public static NotificationSlice of(List<NotificationItem> fetched, int size) {
        boolean hasNext = fetched.size() > size;
        List<NotificationItem> items = hasNext ? List.copyOf(fetched.subList(0, size)) : fetched;
        Long nextCursor = hasNext ? items.getLast().id() : null;
        return new NotificationSlice(items, nextCursor, hasNext);
    }
}
