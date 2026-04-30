package com.recaring.config.infra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SsmEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SSM_PREFIX = "/recaring/";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();

        try (SsmClient ssm = SsmClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build()) {

            String nextToken = null;
            do {
                GetParametersByPathRequest request = GetParametersByPathRequest.builder()
                        .path(SSM_PREFIX)
                        .withDecryption(true)
                        .nextToken(nextToken)
                        .build();

                GetParametersByPathResponse response = ssm.getParametersByPath(request);

                for (Parameter param : response.parameters()) {
                    String key = param.name().substring(SSM_PREFIX.length());
                    properties.put(key, param.value());
                }

                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Exception e) {
            throw new IllegalStateException("[SSM 설정 : 로드 실패] " + e.getMessage(), e);
        }

        environment.getPropertySources().addFirst(new MapPropertySource("ssm", properties));
    }
}
