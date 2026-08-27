package com.recaring.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

@Tag("integration")
@DataJpaTest
@ActiveProfiles("test")
public abstract class AbstractRepositoryTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = SharedTestContainers.POSTGRES;

    @Autowired
    protected TestEntityManager em;

    @BeforeEach
    void setUp() {
        em.flush();
        em.clear();
    }
}
