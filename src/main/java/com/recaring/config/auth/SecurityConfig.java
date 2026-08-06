package com.recaring.config.auth;

import com.recaring.security.filter.AuthExceptionTranslationFilter;
import com.recaring.security.filter.DeviceTokenAuthFilter;
import com.recaring.security.filter.JwtAuthenticationFilter;
import com.recaring.security.handler.JwtAccessDeniedHandler;
import com.recaring.security.handler.JwtAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
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
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
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
                        // SSE 등 async 요청 완료 시 컨테이너가 ASYNC/ERROR 디스패치로 필터 체인을 재실행한다.
                        // 이때 SecurityContext가 비어 있어 AuthorizationFilter가 원래 경로를 다시 인가하려다
                        // Access Denied를 던진다. 내부 디스패치는 인가 대상에서 제외한다 (최초 REQUEST 인가는 유지).
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                mvc.matcher("/api/v1/auth/sign-up"),
                                mvc.matcher("/api/v1/auth/sign-in/local"),
                                mvc.matcher("/api/v1/auth/sign-in/kakao"),
                                mvc.matcher("/api/v1/auth/sign-in/naver"),
                                mvc.matcher("/api/v1/auth/refresh"),
                                mvc.matcher("/api/v1/auth/sign-out"),
                                mvc.matcher("/api/v1/auth/phone/send-code"),
                                mvc.matcher("/api/v1/auth/phone/verify"),
                                // Swagger UI
                                mvc.matcher("/swagger-ui/**"),
                                mvc.matcher("/swagger-ui.html"),
                                mvc.matcher("/v3/api-docs/**"),
                                mvc.matcher("/v3/api-docs")
                        ).permitAll()
                        .requestMatchers(
                                mvc.matcher("/actuator/**"),
                                mvc.matcher("/error")
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
                                mvc.matcher(HttpMethod.POST, "/api/v1/members/phones"),
                                mvc.matcher(HttpMethod.GET,  "/api/v1/location/settings/{wardKey}/collection-interval"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/location/settings/{wardKey}/collection-interval"),
                                // SafeZone (GUARDIAN/MANAGER 모두 MemberRole.GUARDIAN)
                                mvc.matcher(HttpMethod.POST,   "/api/v1/care/wards/{wardKey}/safe-zones"),
                                mvc.matcher(HttpMethod.GET,    "/api/v1/care/wards/{wardKey}/safe-zones"),
                                mvc.matcher(HttpMethod.GET,    "/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}"),
                                mvc.matcher(HttpMethod.PATCH,  "/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}"),
                                mvc.matcher(HttpMethod.DELETE, "/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}")
                        ).hasRole("GUARDIAN")

                        // GUARDIAN + WARD 모두 접근 가능
                        .requestMatchers(
                                // WARD도 케어 초대 등 본인 대상 알림을 푸시로 받기 위해 FCM 토큰을 등록한다
                                mvc.matcher(HttpMethod.PUT,   "/api/v1/notifications/device-tokens"),
                                // WARD는 자신이 관리하는 대상자가 없어 빈 목록을 받는다 (컨트롤러가 역할 무관 처리)
                                mvc.matcher(HttpMethod.GET,   "/api/v1/care/wards"),
                                mvc.matcher(HttpMethod.GET,   "/api/v1/care/requests/received"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/care/requests/{requestKey}/accept"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/care/requests/{requestKey}/reject"),
                                mvc.matcher(HttpMethod.GET,   "/api/v1/care/wards/{wardKey}/caregivers"),
                                mvc.matcher(HttpMethod.GET,   "/api/v1/location/history/{wardKey}"),
                                mvc.matcher(HttpMethod.GET,   "/api/v1/notifications/settings/{wardKey}"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/notifications/settings/{wardKey}/safe-zone"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/notifications/settings/{wardKey}/anomaly"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/notifications/settings/{wardKey}/emergency-call"),
                                mvc.matcher(HttpMethod.PATCH, "/api/v1/notifications/settings/{wardKey}/battery")
                        ).hasAnyRole("GUARDIAN", "WARD")

                        .anyRequest().authenticated())
                .sessionManagement(configurer ->
                        configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(configurer ->
                        configurer
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler))
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
