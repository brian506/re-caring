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

    @Operation(
            summary = "장소 검색",
            description = """
                    키워드로 장소 후보를 최대 5건 조회합니다.
                    위경도를 함께 보내면 해당 반경을 우선 검색합니다. 다만 카카오는 반경을 필터가 아니라 정렬 가중치로 쓰기 때문에
                    반경 안에 후보가 없어도 엉뚱한 장소가 소수 딸려옵니다. 그래서 편향 결과가 3건 미만이거나 키워드와 맞는 장소가
                    하나도 없으면 전국으로 재검색해, 전국 결과를 앞에 두고 편향 결과를 뒤에 붙여 placeId 기준으로 중복을 제거합니다.
                    이때 응답 순서는 거리순이 아니므로 클라이언트는 각 항목의 주소를 반드시 함께 노출해야 합니다.
                    placeId는 카카오 장소 id로, 목록 렌더링 키·선택 상태 관리용입니다. 우리 식별자가 아니므로 영속 키로 쓰지 마세요.
                    """
    )
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
