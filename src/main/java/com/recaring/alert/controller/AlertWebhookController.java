package com.recaring.alert.controller;

import com.recaring.alert.business.AlertService;
import com.recaring.alert.controller.request.AlertWebhookRequest;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert", description = "Prometheus AlertManager 웹훅 수신 및 LLM 자율 조사 API")
public class AlertWebhookController {

    private final AlertService alertService;

    @Operation(
            summary = "Prometheus AlertManager 웹훅 수신",
            description = "AlertManager에서 전송하는 알림을 수신하고 LLM 자율 조사를 비동기로 시작합니다. X-Webhook-Secret 헤더 필수."
    )
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> receiveAlert(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String headerSecret,
            @RequestParam(value = "secret", required = false) String paramSecret,
            @Valid @RequestBody AlertWebhookRequest request
    ) {
        String webhookSecret = headerSecret != null ? headerSecret : paramSecret;
        alertService.receiveWebhook(webhookSecret, request);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
