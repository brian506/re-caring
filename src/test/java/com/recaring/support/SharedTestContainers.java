package com.recaring.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 한 JVM 안에서 컨테이너를 딱 한 번만 띄우고 끝까지 재사용한다.
 *
 * <p>@Testcontainers의 @Container는 테스트 클래스마다 stop/start 하는데,
 * Spring TestContext는 설정이 같으면 컨텍스트(=커넥션 풀)를 캐시해 재사용한다.
 * 그래서 두 번째 클래스부터 재사용된 풀이 이미 죽은 포트를 물고 무한 재시도에 빠진다.
 * 컨테이너를 멈추지 않으면 포트가 유지돼 캐시된 컨텍스트가 그대로 살아 있다.
 * 프로세스가 끝나면 Ryuk이 정리한다.
 */
public final class SharedTestContainers {

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    public static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    private SharedTestContainers() {
    }
}
