package com.recaring.siteinquiry.implement;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.repository.SiteInquiryRepository;
import com.recaring.siteinquiry.vo.SiteInquiryPassword;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteInquiryAuthenticator {

    private final PasswordEncoder passwordEncoder;
    private final SiteInquiryRepository siteInquiryRepository;
    private final SiteInquiryRateLimitValidator rateLimitValidator;

    public String encode(SiteInquiryPassword password) {
        return password == null ? null : passwordEncoder.encode(password.value());
    }

    public void authenticate(String inquiryKey, String rawPassword) {
        SiteInquiry inquiry = siteInquiryRepository.findByInquiryKey(inquiryKey)
                .orElseThrow(() -> new AppException(ErrorType.SITE_INQUIRY_NOT_FOUND));
        if (!inquiry.isSecret()) {
            return;
        }
        rateLimitValidator.reserveViewAttempt(inquiryKey);
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, inquiry.getPasswordHash())) {
            log.info("[WebInquiry : SecretPasswordMismatch]: inquiryKey={}", inquiryKey);
            throw new AppException(ErrorType.SITE_INQUIRY_PASSWORD_MISMATCH);
        }
        rateLimitValidator.releaseViewAttempt(inquiryKey);
    }
}
