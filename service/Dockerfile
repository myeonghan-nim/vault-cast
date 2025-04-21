FROM gradle:8.5-jdk21 AS builder

WORKDIR /home/gradle/project

COPY --chown=gradle:gradle . .

RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-noble

WORKDIR /app

COPY --from=builder /home/gradle/project/service/build/libs/service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8444

ENTRYPOINT ["java","-Dspring.config.location=classpath:/application.properties","-jar","app.jar"]
