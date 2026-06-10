package com.recaring.device.implement;

import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WardDeviceTokenReader {

    private final WardDeviceTokenRepository wardDeviceTokenRepository;

    @Cacheable(cacheNames = "deviceToken", key = "#token")
    public String getByToken(String token) {
        return wardDeviceTokenRepository.findByToken(token)
                .map(entity -> entity.getWardKey())
                .orElseThrow(() -> new AppException(ErrorType.INVALID_DEVICE_TOKEN));
    }

    @CacheEvict(cacheNames = "deviceToken", key = "#token")
    public void evict(String token) {
        // cache eviction only — called on token reissue
    }
}
