# ---- Giai đoạn 1: build jar bằng Gradle wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# ---- Giai đoạn 2: runtime JRE gọn nhẹ ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
