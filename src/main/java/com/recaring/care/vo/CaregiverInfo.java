package com.recaring.care.vo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CaregiverInfo(
        String memberKey,
        String name,
        String phone,
        CareRole careRole,
        String profileAvatarCode,
        String designatedProfileAvatarCode,
        String wardNickname
) {

    public String wardDisplayName(String wardName) {
        return wardNickname == null ? wardName : wardNickname;
    }

    public static Map<String, List<String>> groupKeysByWardDisplayName(List<CaregiverInfo> caregivers, String wardName) {
        return caregivers.stream()
                .collect(Collectors.groupingBy(
                        caregiver -> caregiver.wardDisplayName(wardName),
                        LinkedHashMap::new,
                        Collectors.mapping(CaregiverInfo::memberKey, Collectors.toList())));
    }
}
