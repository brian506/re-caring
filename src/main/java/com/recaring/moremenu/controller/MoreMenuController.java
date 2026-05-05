package com.recaring.moremenu.controller;

import com.recaring.moremenu.business.MoreMenuService;
import com.recaring.moremenu.controller.response.MoreMenuResponse;
import com.recaring.security.vo.AuthMember;
import com.recaring.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/more")
@RequiredArgsConstructor
@Tag(name = "More", description = "More menu API")
public class MoreMenuController {

    private final MoreMenuService moreMenuService;

    @Operation(
            summary = "Get more menu",
            description = "Returns the ST-001 more menu for the authenticated member. Guardian accounts must pass wardKey."
    )
    @GetMapping("/menu")
    public ResponseEntity<ApiResponse<MoreMenuResponse>> getMenu(
            @Parameter(hidden = true)
            @AuthMember String memberKey,
            @Parameter(description = "Ward member key. Required for guardian and manager contexts.", required = false)
            @RequestParam(required = false) String wardKey
    ) {
        MoreMenuResponse response = MoreMenuResponse.from(moreMenuService.getMenu(memberKey, wardKey));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

