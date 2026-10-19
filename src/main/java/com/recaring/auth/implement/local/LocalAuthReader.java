package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocalAuthReader {

    private final LocalAuthRepository authRepository;

    public EncodedPassword findPasswordByMemberKey(String memberKey) {
        return findOptionalPasswordByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Optional<EncodedPassword> findOptionalPasswordByMemberKey(String memberKey) {
        return authRepository.findByMemberKey(memberKey)
                .map(LocalAuth::getPassword)
                .map(EncodedPassword::new);
    }
}
