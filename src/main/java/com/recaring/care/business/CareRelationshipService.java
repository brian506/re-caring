package com.recaring.care.business;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.WardInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareRelationshipService {

    private final CareRelationshipReader careRelationshipReader;
    private final CareRelationshipValidator careRelationshipValidator;

    public List<WardInfo> getMyWards(String memberKey) {
        return careRelationshipReader.findWardInfos(memberKey);
    }

    public List<CaregiverInfo> getCaregivers(String wardKey, String requesterKey) {
        careRelationshipValidator.validateCaregiverViewAccess(requesterKey, wardKey);
        return careRelationshipReader.findCaregiverInfos(wardKey);
    }
}
