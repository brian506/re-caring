FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 모든 타임존 통일
ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/recaring-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080
EXPOSE 9010

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]