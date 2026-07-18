FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache wget
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /app/target/ps-onboarding-svc-*.jar app.jar
RUN chown spring:spring app.jar && mkdir -p /tmp/logs
USER spring:spring

ENV PORT=8080
EXPOSE ${PORT}

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/onboarding-api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java -jar -Dserver.port=${PORT} app.jar"]
