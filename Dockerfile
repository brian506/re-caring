FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 모든 타임존 통일
ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/recaring-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]