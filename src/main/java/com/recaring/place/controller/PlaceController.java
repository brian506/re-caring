package com.recaring.place.controller;

import com.recaring.place.business.PlaceService;
import com.recaring.place.controller.response.PlaceResponse;
import com.recaring.place.vo.PlaceSearchCondition;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@Tag(name = "Place", description = "장소 검색 API (안심존 등록용)")
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "장소 검색", description = "키워드로 장소 후보를 최대 5건 조회합니다. 위경도를 함께 보내면 해당 반경을 우선 검색하고, 결과가 없으면 전국으로 재검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> searchPlaces(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Integer radiusMeters
    ) {
        PlaceSearchCondition condition = PlaceSearchCondition.of(query, latitude, longitude, radiusMeters);

        List<PlaceResponse> responses = placeService.searchPlaces(condition)
                .stream()
                .map(PlaceResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
