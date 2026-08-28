package com.recaring.support;

import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.TokenPayload;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Date;

@IntegrationTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = SharedTestContainers.POSTGRES;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", SharedTestContainers.REDIS::getHost);
        registry.add("spring.data.redis.port", () -> SharedTestContainers.REDIS.getMappedPort(6379));
    }

    @LocalServerPort
    protected int port;

    @Autowired
    private JwtGenerator jwtGenerator;

    protected RestTestClient client;

    @BeforeEach
    void initClient() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    protected String bearerToken(String memberKey, MemberRole role) {
        return "Bearer " + jwtGenerator.generateJwt(new TokenPayload(memberKey, role, new Date())).accessToken();
    }
}
