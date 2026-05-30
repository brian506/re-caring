package com.recaring.alert.controller;

import com.recaring.alert.business.AlertResolutionService;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/slack")
@RequiredArgsConstructor
@Tag(name = "Alert", description = "Slack 인터랙티브 액션 처리 API")
public class SlackActionController {

    private final AlertResolutionService alertResolutionService;

    @Operation(
            summary = "Slack 인터랙티브 액션 처리",
            description = "Slack 버튼/메뉴 등 인터랙티브 액션을 수신합니다. X-Slack-Request-Timestamp, X-Slack-Signature 헤더 필수."
    )
    @PostMapping("/actions")
    public ResponseEntity<ApiResponse<Void>> handleSlackAction(
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestHeader("X-Slack-Signature") String signature,
            @RequestBody String body
    ) {
        alertResolutionService.handleSlackAction(timestamp, signature, body);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
