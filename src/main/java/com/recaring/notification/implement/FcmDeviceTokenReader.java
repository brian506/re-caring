package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FcmDeviceTokenReader {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    public List<FcmDeviceToken> findTokensByCareRoles(
            Collection<String> guardianMemberKeys,
            Collection<String> managerMemberKeys
    ) {
        return fcmDeviceTokenRepository.findTokensByCareRoles(guardianMemberKeys, managerMemberKeys);
    }

    public List<FcmDeviceToken> findTokensByMemberKey(String memberKey) {
        return fcmDeviceTokenRepository.findByMemberKey(memberKey);
    }
}
