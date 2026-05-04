package com.recaring.config.infra;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservationPredicate noActuatorObservations() {
        return (name, context) -> {
            if ("http.server.requests".equals(name) && context instanceof ServerRequestObservationContext ctx) {
                return !ctx.getCarrier().getRequestURI().startsWith("/actuator");
            }
            return true;
        };
    }
}
