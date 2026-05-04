package com.recaring.device.implement;

import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WardDeviceTokenReader {

    private final WardDeviceTokenRepository wardDeviceTokenRepository;

    public String getByToken(String token) {
        return wardDeviceTokenRepository.findByToken(token)
                .map(entity -> entity.getWardKey())
                .orElseThrow(() -> new AppException(ErrorType.INVALID_DEVICE_TOKEN));
    }
}
