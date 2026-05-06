package com.recaring.config.auth;

import com.recaring.security.filter.AuthExceptionTranslationFilter;
import com.recaring.security.filter.DeviceTokenAuthFilter;
import com.recaring.security.filter.JwtAuthenticationFilter;
import com.recaring.security.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${client.url}")
    private String clientUrl;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AuthExceptionTranslationFilter authExceptionTranslationFilter;
    private final DeviceTokenAuthFilter deviceTokenAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder mvc = withDefaults();
        return http
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                mvc.matcher("/api/v1/auth/sign-up"),
                                mvc.matcher("/api/v1/auth/sign-in/local"),
                                mvc.matcher("/api/v1/auth/sign-in/kakao"),
                                mvc.matcher("/api/v1/auth/sign-in/naver"),
                                mvc.matcher("/api/v1/auth/sign-up/kakao"),
                                mvc.matcher("/api/v1/auth/sign-up/naver"),
                                mvc.matcher("/api/v1/auth/refresh"),
                                mvc.matcher("/api/v1/auth/sign-out"),
                                mvc.matcher("/api/v1/auth/email"),
                                mvc.matcher("/api/v1/auth/value"),
                                // Swagger UI
                                mvc.matcher("/swagger-ui/**"),
                                mvc.matcher("/swagger-ui.html"),
                                mvc.matcher("/v3/api-docs/**"),
                                mvc.matcher("/v3/api-docs")
                        ).permitAll()
                        .requestMatchers(
                                mvc.matcher("/actuator/**")
                        ).permitAll()

                        // WARD 전용
                        .requestMatchers(
                                mvc.matcher(HttpMethod.POST, "/api/v1/location/gps"),
                                mvc.matcher(HttpMethod.GET,  "/api/v1/location/settings/collection-interval/me"),
                                mvc.matcher(HttpMethod.POST, "/api/v1/device/token")
                        ).hasRole("WARD")

                        // GUARDIAN 전용 (보호자만 접근 가능)
                        .requestMatchers(
                                mvc.matcher(HttpMethod.POST, "/api/v1/care/requests"),
                                mvc.matcher(HttpMethod.GET,  "/api/v1/care/wards"),
                                mvc.matcher(HttpMethod.POST, "/api/v1/members/phones"),
                                mvc.matcher(HttpMethod.GET,  "/api/v1/location/settings/{wardKey}/collection-interval"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/location/settings/{wardKey}/collection-interval")
                        ).hasRole("GUARDIAN")

                        // GUARDIAN + WARD 모두 접근 가능
                        .requestMatchers(
                                mvc.matcher(HttpMethod.GET,   "/api/v1/care/requests/received"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/care/requests/{requestKey}/accept"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/care/requests/{requestKey}/reject"),
                                mvc.matcher(HttpMethod.GET,   "/api/v1/care/wards/{wardKey}/caregivers")
                        ).hasAnyRole("GUARDIAN", "WARD")

                        .anyRequest().authenticated())
                .sessionManagement(configurer ->
                        configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(configurer ->
                        configurer.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(deviceTokenAuthFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(authExceptionTranslationFilter, DeviceTokenAuthFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(clientUrl));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
