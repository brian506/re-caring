package com.recaring.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.support.exception.AppException;
import com.recaring.support.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthExceptionTranslationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        AppException exception = (AppException) request.getAttribute(JwtAuthenticationFilter.EXCEPTION_ATTRIBUTE);
        if (exception != null) {
            writeErrorResponse(response, exception);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, AppException exception) throws IOException {
        response.setStatus(exception.getErrorType().getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String body = objectMapper.writeValueAsString(ApiResponse.error(exception.getErrorType()));
        response.getWriter().write(body);
    }
}
