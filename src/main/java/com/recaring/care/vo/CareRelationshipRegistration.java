package com.recaring.care.vo;

import com.recaring.care.dataaccess.entity.CareRole;

public record CareRelationshipRegistration(
        String wardMemberKey,
        String caregiverKey,
        CareRole careRole
) {}
