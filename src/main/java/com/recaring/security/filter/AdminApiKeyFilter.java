package com.recaring.security.filter;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    public static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";
    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String ADMIN_PRINCIPAL = "admin";

    private final byte[] adminApiKey;

    public AdminApiKeyFilter(@Value("${admin.api-key:}") String adminApiKey) {
        this.adminApiKey = adminApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(ADMIN_KEY_HEADER);
        if (adminApiKey.length == 0 || !StringUtils.hasText(header)
                || !MessageDigest.isEqual(adminApiKey, header.getBytes(StandardCharsets.UTF_8))) {
            throw new AppException(ErrorType.REQUIRED_AUTH);
        }

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ADMIN_PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        filterChain.doFilter(request, response);
    }
}
