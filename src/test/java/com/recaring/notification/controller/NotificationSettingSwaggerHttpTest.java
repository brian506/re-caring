package com.recaring.notification.controller;

import com.recaring.config.swagger.SwaggerConfig;
import com.recaring.notification.business.NotificationSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSecurityConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = NotificationSettingSwaggerHttpTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "server.address=127.0.0.1",
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
                        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration," +
                        "org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration," +
                        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration," +
                        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration," +
                        "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration," +
                        "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration"
        }
)
@DisplayName("NotificationSetting Swagger HTTP test")
class NotificationSettingSwaggerHttpTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("OpenAPI docs expose notification setting paths")
    void apiDocs_contains_notification_setting_paths() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"title\":\"re;caRing API\"");
        assertThat(response.body()).contains("\"BearerAuth\"");
        assertThat(response.body()).contains("\"/api/v1/notifications/settings/{wardKey}\"");
        assertThat(response.body()).contains("\"summary\":\"Get notification settings\"");
        assertThat(response.body()).contains("\"/api/v1/notifications/settings/{wardKey}/safe-zone\"");
        assertThat(response.body()).contains("\"summary\":\"Update safe zone notification settings\"");
        assertThat(response.body()).contains("\"/api/v1/notifications/settings/{wardKey}/anomaly\"");
        assertThat(response.body()).contains("\"summary\":\"Update anomaly notification settings\"");
        assertThat(response.body()).contains("\"/api/v1/notifications/settings/{wardKey}/emergency-call\"");
        assertThat(response.body()).contains("\"summary\":\"Update emergency call notification settings\"");
        assertThat(response.body()).contains("\"/api/v1/notifications/settings/{wardKey}/battery\"");
        assertThat(response.body()).contains("\"summary\":\"Update battery notification settings\"");
    }

    @Test
    @DisplayName("Swagger UI index is accessible")
    void swaggerUi_is_accessible() throws Exception {
        HttpResponse<String> response = get("/swagger-ui/index.html");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Swagger UI");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            NotificationSettingController.class,
            SwaggerConfig.class
    })
    @ImportAutoConfiguration(classes = {
            SpringDocConfiguration.class,
            SpringDocSecurityConfiguration.class,
            SpringDocConfigProperties.class,
            SpringDocWebMvcConfiguration.class,
            org.springdoc.webmvc.ui.SwaggerConfig.class,
            SwaggerUiConfigProperties.class,
            SwaggerUiOAuthProperties.class
    })
    static class TestApplication {

        @Bean
        NotificationSettingService notificationSettingService() {
            return Mockito.mock(NotificationSettingService.class);
        }
    }
}
