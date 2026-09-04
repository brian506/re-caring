package com.recaring.care.controller.request;

import com.recaring.care.dataaccess.entity.CareRole;
import jakarta.validation.constraints.NotNull;

public record UpdateCareRoleRequest(
        @NotNull CareRole careRole
) {
}
