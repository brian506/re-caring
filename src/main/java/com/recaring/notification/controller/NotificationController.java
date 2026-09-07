package com.recaring.notification.controller;

import com.recaring.notification.business.NotificationService;
import com.recaring.notification.controller.response.NotificationSliceResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification", description = "Notification inbox API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "내 알림 목록 조회",
            description = """
                    로그인한 회원의 알림 목록을 최신순으로 반환합니다. 커서 기반 페이징입니다.
                    첫 페이지는 cursor 없이 요청하고, 이후에는 직전 응답의 nextCursor를 그대로 넘깁니다.
                    hasNext가 false이면 마지막 페이지이며 nextCursor는 null입니다.
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSliceResponse>> getMyNotifications(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @Parameter(description = "직전 응답의 nextCursor. 첫 페이지는 생략합니다.")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기 (1~50)")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        NotificationSliceResponse response = NotificationSliceResponse.from(
                notificationService.getMyNotifications(memberKey, cursor, size)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
