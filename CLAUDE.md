# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 및 실행 명령어

```bash
docker compose -f docker-compose-local.yml up -d  # 인프라 (PostgreSQL 16, Redis 7)
./gradlew build                # 빌드
./gradlew test                 # 단위 테스트
./gradlew integrationTest      # 통합 테스트 (Testcontainers)
./gradlew test --tests "com.recaring.XxxTest"  # 특정 클래스
./gradlew clean compileJava    # QueryDSL Q클래스 재생성
```

로컬 실행 시 Spring 프로파일을 `local`로 지정해야 한다 (`application-local.yml` 참고).

## 스택

Spring Boot 4.0.3 · Java 21 · PostgreSQL 16 · Redis 7 · QueryDSL 7 · JWT (jjwt 0.12) · CoolSMS
