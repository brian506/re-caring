package com.recaring.notification.implement.fcm;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.vo.FcmDeviceTokenInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FcmDeviceTokenReader {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    public List<FcmDeviceTokenInfo> findGuardianTokens(Collection<String> memberKeys) {
        return toInfos(fcmDeviceTokenRepository.findGuardianTokens(memberKeys));
    }

    public List<FcmDeviceTokenInfo> findTokensByMemberKey(String memberKey) {
        return toInfos(fcmDeviceTokenRepository.findByMemberKey(memberKey));
    }

    private List<FcmDeviceTokenInfo> toInfos(List<FcmDeviceToken> tokens) {
        return tokens.stream()
                .map(FcmDeviceTokenInfo::from)
                .toList();
    }
}
