package com.recaring.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.device.implement.WardDeviceTokenReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import com.recaring.support.response.ApiResponse;
import com.recaring.support.response.ResultType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeviceTokenAuthFilter extends OncePerRequestFilter {

    private static final String DEVICE_PREFIX = "Device ";
    private static final String GPS_PATH = "/api/v1/location/gps";
    private static final String LOCATION_INTERVAL_ME_PATH = "/api/v1/location/settings/collection-interval/me";

    private final WardDeviceTokenReader wardDeviceTokenReader;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isDeviceAuthenticatedEndpoint(request);
    }

    private boolean isDeviceAuthenticatedEndpoint(HttpServletRequest request) {
        return (GPS_PATH.equals(request.getRequestURI())
                && HttpMethod.POST.name().equals(request.getMethod()))
                || (LOCATION_INTERVAL_ME_PATH.equals(request.getRequestURI())
                && HttpMethod.GET.name().equals(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (!StringUtils.hasText(header) || !header.startsWith(DEVICE_PREFIX)) {
            writeUnauthorized(response);
            return;
        }

        String token = header.substring(DEVICE_PREFIX.length());

        try {
            String wardKey = wardDeviceTokenReader.getByToken(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    wardKey, null, List.of(new SimpleGrantedAuthority("ROLE_WARD"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AppException e) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(ErrorType.INVALID_DEVICE_TOKEN));
    }
}
