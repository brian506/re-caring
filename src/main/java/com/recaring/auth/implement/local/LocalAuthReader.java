package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocalAuthReader {

    private final LocalAuthRepository authRepository;

    public LocalAuth findByMemberKey(String memberKey) {
        return authRepository.findByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Optional<LocalAuth> findOptionalByMemberKey(String memberKey) {
        return authRepository.findByMemberKey(memberKey);
    }
}
