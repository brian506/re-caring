package com.recaring.location.controller;

import com.recaring.location.business.LocationSettingService;
import com.recaring.location.controller.request.UpdateLocationCollectionIntervalRequest;
import com.recaring.location.controller.response.LocationCollectionIntervalSettingResponse;
import com.recaring.location.controller.response.WardLocationCollectionIntervalResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/location/settings")
@RequiredArgsConstructor
@Tag(name = "Location Setting", description = "Location collection setting API")
public class LocationSettingController {

    private final LocationSettingService locationSettingService;

    @Operation(
            summary = "Get location collection interval",
            description = "Returns the current location collection interval and selectable options. [GUARDIAN only]"
    )
    @GetMapping("/{wardKey}/collection-interval")
    public ResponseEntity<ApiResponse<LocationCollectionIntervalSettingResponse>> getCollectionInterval(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey
    ) {
        LocationCollectionIntervalSettingResponse response =
                LocationCollectionIntervalSettingResponse.from(
                        locationSettingService.getCollectionInterval(memberKey, wardKey)
                );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Update location collection interval",
            description = "Updates the location collection interval for a ward. [GUARDIAN only]"
    )
    @PatchMapping("/{wardKey}/collection-interval")
    public ResponseEntity<ApiResponse<Void>> updateCollectionInterval(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @PathVariable String wardKey,
            @Valid @RequestBody UpdateLocationCollectionIntervalRequest request
    ) {
        locationSettingService.updateCollectionInterval(memberKey, request.toCommand(wardKey));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "Get my location collection interval",
            description = "Returns only the current interval for the ward device app. [WARD Device Token only]"
    )
    @GetMapping("/collection-interval/me")
    public ResponseEntity<ApiResponse<WardLocationCollectionIntervalResponse>> getMyCollectionInterval(
            @Parameter(hidden = true)
            @AuthMember String wardKey
    ) {
        WardLocationCollectionIntervalResponse response =
                WardLocationCollectionIntervalResponse.from(
                        locationSettingService.getMyCollectionInterval(wardKey)
                );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
