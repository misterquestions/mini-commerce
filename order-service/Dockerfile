# ==== Build stage ====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Leverage Docker layer caching for Gradle deps
COPY gradlew gradlew
COPY gradle gradle
RUN chmod +x gradlew

# Copy build scripts first to warm cache
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now copy the sources
COPY src src
RUN ./gradlew --no-daemon clean bootJar

# ==== Run stage ====
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
# Non-root user for better security
RUN useradd -ms /bin/bash appuser
USER appuser

# Copy built jar
COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
