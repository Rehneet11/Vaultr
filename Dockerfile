# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl gettext-base \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system vaultr \
    && useradd --system --gid vaultr --home-dir /app --shell /usr/sbin/nologin vaultr

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/vaultr.jar
COPY docker/sharding.yml.template /app/config/sharding.yml.template
COPY docker/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh \
    && mkdir -p /app/tmp \
    && chown -R vaultr:vaultr /app

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=65 -XX:InitialRAMPercentage=25 -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:${SERVER_PORT}/actuator/health >/dev/null || exit 1

USER vaultr

ENTRYPOINT ["/app/docker-entrypoint.sh"]
