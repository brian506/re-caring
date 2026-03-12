package com.recaring.security.filter;

import com.recaring.domain.member.MemberRole;
import com.recaring.security.jwt.JwtValidator;
import com.recaring.support.exception.AppException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    static final String EXCEPTION_ATTRIBUTE = "jwtException";

    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Jws<Claims> claimsJws = jwtValidator.validate(token);
                Claims claims = claimsJws.getPayload();

                String memberKey = claims.getSubject();
                String role = claims.get("role", String.class);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(memberKey, null, List.of(authority));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AppException e) {
                request.setAttribute(EXCEPTION_ATTRIBUTE, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
