package com.recaring.security.filter;

import com.recaring.security.exception.AuthenticationExceptionHandler;
import com.recaring.support.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthExceptionTranslationFilter extends OncePerRequestFilter {

    private final AuthenticationExceptionHandler handler;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (AppException e) {
            handler.handle(request, response, e);

        }


    }
}
